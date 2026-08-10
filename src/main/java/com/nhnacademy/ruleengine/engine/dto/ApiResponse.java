package com.nhnacademy.ruleengine.engine.dto;

import com.nhnacademy.ruleengine.global.dto.ErrorDetail;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
        LocalDateTime timestamp
) {
    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), LocalDateTime.now());
    }
}
