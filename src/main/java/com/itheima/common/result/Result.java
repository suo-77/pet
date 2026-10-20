package com.itheima.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回格式 {code, msg, data}
 * code=0 成功；401 未登录/token失效；403 无权限；其他为业务或系统错误
 */
@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String msg;
    private T data;

    public Result() {
    }

    public Result(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    /** 成功：携带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMsg(), data);
    }

    /** 成功：无数据 */
    public static <T> Result<T> success() {
        return success(null);
    }

    /** 失败：使用默认错误码与提示 */
    public static <T> Result<T> error(String msg) {
        return new Result<>(ResultCode.SYSTEM_ERROR.getCode(), msg, null);
    }

    /** 失败：自定义错误码 */
    public static <T> Result<T> error(Integer code, String msg) {
        return new Result<>(code, msg, null);
    }
}
