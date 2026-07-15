package com.nhnacademy.ruleengine.global.exception;


import org.springframework.validation.FieldError;

import java.util.List;

public record ErrorDetail(
        String code,
        String message,
        List<FieldError> fieldErrors
) {

    public ErrorDetail(
            String code,
            String message
    ) {
        this(code, message, List.of());
    }

    public ErrorDetail {
        fieldErrors = fieldErrors == null
                ? List.of()
                : List.copyOf(fieldErrors);
    }
}

