package com.nhnacademy.ruleengine.global.exception;

import lombok.Getter;
import okhttp3.internal.http2.ErrorCode;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorCode errorCode;

    public BaseException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}