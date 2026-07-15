package com.nhnacademy.ruleengine.global.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorDetail error,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> successWithoutData() {
        return new ApiResponse<>(true, null, null, LocalDateTime.now());
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorDetail(code, message), LocalDateTime.now());
    }

    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, new ErrorDetail(errorCode.getCode(), errorCode.getMessage()), LocalDateTime.now());
    }

    public record ErrorDetail(String code, String message, List<FieldError> fieldErrors) {
        public ErrorDetail(String code, String message) {
            this(code, message, List.of());
        }
    }

    public record FieldError(String field, String reason) {
    }

}
