package com.itheima.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.itheima.common.constant.RedisKeyConstant;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.ResultCode;
import com.itheima.common.util.JwtUtil;
import com.itheima.common.util.PasswordUtil;
import com.itheima.common.util.XssUtil;
import com.itheima.dto.LoginDTO;
import com.itheima.dto.RegisterDTO;
import com.itheima.dto.SmsLoginDTO;
import com.itheima.entity.User;
import com.itheima.mapper.UserMapper;
import com.itheima.mq.constant.MQConstant;
import com.itheima.mq.stream.MQProducer;
import com.itheima.service.AuthService;
import com.itheima.vo.LoginVO;
import com.itheima.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * 账号服务实现：
 * 1. 验证码：Redis 60s 频率限制 + 5分钟有效期，主接口只写 Redis 和投递 Stream，短信异步发送
 * 2. 登录态：JWT 生成 token，Redis 存 login:token:{token} -> userId（删除即失效）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;
    private final MQProducer mqProducer;

    /** token 有效期（小时），与 Redis TTL 保持一致 */
    @Value("${pet.jwt.expire-hours}")
    private Long expireHours;

    @Override
    public void sendSmsCode(String phone) {
        // 频率限制：60 秒内只允许发送一次
        Boolean pass = stringRedisTemplate.opsForValue()
                .setIfAbsent(RedisKeyConstant.SMS_LIMIT + phone, "1", Duration.ofSeconds(60));
        if (Boolean.FALSE.equals(pass)) {
            throw new BusinessException(ResultCode.RATE_LIMIT, "发送太频繁，请60秒后再试");
        }
        // 生成 6 位数字验证码，写入 Redis，5 分钟有效
        String code = RandomUtil.randomNumbers(6);
        stringRedisTemplate.opsForValue()
                .set(RedisKeyConstant.SMS_CODE + phone, code, Duration.ofMinutes(5));
        // 投递 Stream，由消费者异步发送短信，主接口立即返回
        mqProducer.send(MQConstant.SMS_STREAM, Map.of("phone", phone, "code", code));
        log.info("验证码已写入Redis并投递异步发送: phone={}", phone);
    }

    @Override
    public LoginVO register(RegisterDTO dto) {
        // 账号唯一校验
        if (userMapper.findByUsername(dto.getUsername()) != null) {
            throw new BusinessException("账号已被注册");
        }
        // 绑定手机号时必须校验验证码
        checkSmsCodeOnRegister(dto.getPhone(), dto.getCode());
        // 组装用户：密码加盐MD5存储，昵称默认生成
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encode(dto.getPassword()));
        user.setPhone(StrUtil.blankToDefault(dto.getPhone(), null));
        user.setNickname(StrUtil.blankToDefault(XssUtil.clean(dto.getNickname()),
                "用户" + RandomUtil.randomNumbers(4)));
        user.setAvatar(dto.getAvatar());
        user.setIntro("这个人很懒，什么都没有留下~");
        userMapper.insert(user);
        log.info("用户注册成功: userId={}, username={}", user.getId(), user.getUsername());
        return createLoginVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        checkUserStatus(user);
        return createLoginVO(user);
    }

    @Override
    public LoginVO loginBySms(SmsLoginDTO dto) {
        // 校验验证码，防止爆破：每次校验无论成败均消耗
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisKeyConstant.SMS_CODE + dto.getPhone());
        if (cacheCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        stringRedisTemplate.delete(RedisKeyConstant.SMS_CODE + dto.getPhone());
        if (!cacheCode.equals(dto.getCode())) {
            throw new BusinessException("验证码错误");
        }
        // 有账号直接登录，无账号自动注册
        User user = userMapper.findByPhone(dto.getPhone());
        if (user == null) {
            user = new User();
            user.setUsername(dto.getPhone());
            user.setPassword(PasswordUtil.encode(RandomUtil.randomNumbers(12)));
            user.setPhone(dto.getPhone());
            user.setNickname("用户" + StrUtil.sub(dto.getPhone(), 7, 11));
            user.setIntro("这个人很懒，什么都没有留下~");
            userMapper.insert(user);
            log.info("短信登录自动注册: userId={}, phone={}", user.getId(), user.getPhone());
        }
        checkUserStatus(user);
        return createLoginVO(user);
    }

    @Override
    public void logout(String token) {
        stringRedisTemplate.delete(RedisKeyConstant.LOGIN_TOKEN + token);
        log.info("用户退出登录，token已删除");
    }

    @Override
    public UserVO info(Long userId) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return UserVO.from(user);
    }

    /** 注册时若填写手机号，则必须通过短信验证码校验 */
    private void checkSmsCodeOnRegister(String phone, String code) {
        if (StrUtil.isBlank(phone)) {
            return;
        }
        if (StrUtil.isBlank(code)) {
            throw new BusinessException("绑定手机号需要填写短信验证码");
        }
        String cacheCode = stringRedisTemplate.opsForValue().get(RedisKeyConstant.SMS_CODE + phone);
        if (cacheCode == null) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        stringRedisTemplate.delete(RedisKeyConstant.SMS_CODE + phone);
        if (!cacheCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }
    }

    /** 校验账号状态 */
    private void checkUserStatus(User user) {
        if (user.getStatus() != null && user.getStatus() == 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用");
        }
    }

    /**
     * 生成登录态：JWT token + Redis 存储映射（TTL 与 token 有效期一致）
     */
    private LoginVO createLoginVO(User user) {
        String token = jwtUtil.createToken(user.getId());
        stringRedisTemplate.opsForValue().set(RedisKeyConstant.LOGIN_TOKEN + token,
                String.valueOf(user.getId()), Duration.ofHours(expireHours));
        return new LoginVO(token, UserVO.from(user));
    }
}
