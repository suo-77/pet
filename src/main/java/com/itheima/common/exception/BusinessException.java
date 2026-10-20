package com.itheima.common.exception;

import com.itheima.common.result.ResultCode;
import lombok.Getter;

/**
 * 业务异常：service 层校验失败时抛出，由全局异常处理器统一转换成 Result
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.SYSTEM_ERROR.getCode();
    }

    public BusinessException(ResultCode resultCode, String message) {
        super(message);
        this.code = resultCode.getCode();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
