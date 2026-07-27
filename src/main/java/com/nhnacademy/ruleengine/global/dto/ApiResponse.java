package com.nhnacademy.ruleengine.global.dto;


import com.nhnacademy.ruleengine.global.exception.ErrorCode;

import java.time.LocalDateTime;


public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> successNodata() {
        return new ApiResponse<>(true, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), LocalDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorDetail(errorCode.name(), errorCode.getMessage()), LocalDateTime.now());
    }
}

