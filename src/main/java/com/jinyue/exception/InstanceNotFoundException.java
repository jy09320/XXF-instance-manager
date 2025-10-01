package com.jinyue.exception;

/**
 * 实例不存在异常
 * 当通过QQ号或UUID查找实例失败时抛出
 */
public class InstanceNotFoundException extends RuntimeException {

    public InstanceNotFoundException(String message) {
        super(message);
    }

    public InstanceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
