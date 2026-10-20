package com.itheima.common.util;

import cn.hutool.jwt.JWT;
import com.itheima.common.exception.BusinessException;
import com.itheima.common.result.ResultCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具：生成 / 解析 token
 * token 本身无状态，登录态由 Redis 存储的 token 决定（token 被 Redis 过期/删除即失效）
 */
@Component
public class JwtUtil {

    @Value("${pet.jwt.secret}")
    private String secret;

    @Value("${pet.jwt.expire-hours}")
    private Long expireHours;

    /** 生成 token，payload 携带 userId */
    public String createToken(Long userId) {
        Date expireAt = new Date(System.currentTimeMillis() + expireHours * 3600 * 1000);
        return JWT.create()
                .setPayload("userId", userId)
                .setExpiresAt(expireAt)
                .setKey(secret.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    /**
     * 校验并解析 token，返回 userId；签名错误或已过期抛 401
     */
    public Long parseToken(String token) {
        try {
            JWT jwt = JWT.of(token);
            if (!jwt.setKey(secret.getBytes(StandardCharsets.UTF_8)).validate(0)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "登录已过期，请重新登录");
            }
            Object userId = jwt.getPayload("userId");
            if (userId == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED, "无效token");
            }
            return Long.valueOf(userId.toString());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "无效token");
        }
    }
}
