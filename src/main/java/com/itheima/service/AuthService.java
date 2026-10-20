package com.itheima.service;

import com.itheima.dto.LoginDTO;
import com.itheima.dto.RegisterDTO;
import com.itheima.dto.SmsLoginDTO;
import com.itheima.vo.LoginVO;
import com.itheima.vo.UserVO;

/**
 * 账号服务：注册 / 登录 / 短信验证码 / 登出 / 个人信息
 */
public interface AuthService {

    /** 发送短信验证码：限流 + 写 Redis(5分钟) + Stream 异步发送 */
    void sendSmsCode(String phone);

    /** 账号密码注册，成功返回登录态 */
    LoginVO register(RegisterDTO dto);

    /** 账号密码登录 */
    LoginVO login(LoginDTO dto);

    /** 短信验证码登录，无账号自动注册 */
    LoginVO loginBySms(SmsLoginDTO dto);

    /** 退出登录：删除 Redis 中的 token */
    void logout(String token);

    /** 查询当前登录用户信息（脱敏） */
    UserVO info(Long userId);
}
