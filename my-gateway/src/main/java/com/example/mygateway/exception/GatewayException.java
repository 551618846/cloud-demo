package com.example.mygateway.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义Gateway业务异常
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GatewayException extends RuntimeException {

    private int code;

    public GatewayException(int code, String message) {
        super(message);
        this.code = code;
    }

    public GatewayException(String message) {
        super(message);
        this.code = 500;
    }

    public GatewayException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
