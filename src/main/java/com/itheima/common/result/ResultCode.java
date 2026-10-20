package com.itheima.common.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 统一业务状态码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(0, "ok"),
    PARAM_ERROR(400, "参数校验失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限操作"),
    NOT_FOUND(404, "资源不存在"),
    RATE_LIMIT(429, "操作过于频繁，请稍后再试"),
    SYSTEM_ERROR(500, "系统繁忙，请稍后再试");

    private final Integer code;
    private final String msg;
}
