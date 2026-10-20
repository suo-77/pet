package com.itheima.common.context;

/**
 * 当前登录用户上下文：Token 拦截器校验通过后写入 userId，业务层随时读取
 */
public final class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    /** 业务层要求必须登录时使用，未登录视为无效 token */
    public static Long getUserIdRequired() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new com.itheima.common.exception.BusinessException(
                    com.itheima.common.result.ResultCode.UNAUTHORIZED, "请先登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
    }
}
