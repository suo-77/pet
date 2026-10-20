package com.itheima.common.util;

import cn.hutool.crypto.SecureUtil;

/**
 * 密码工具：MD5 加盐存储（演示项目使用；生产环境建议 BCrypt）
 * 盐值固定拼接，保证演示数据可通过 SQL 直接初始化
 */
public final class PasswordUtil {

    /** 密码盐值 */
    private static final String SALT = "pet_community_salt";

    private PasswordUtil() {
    }

    /** 加密 */
    public static String encode(String rawPassword) {
        return SecureUtil.md5(rawPassword + SALT);
    }

    /** 校验 */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encode(rawPassword).equals(encodedPassword);
    }
}
