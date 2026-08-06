package com.nhnacademy.ruleengine.global.dto;

import java.util.List;

public record ErrorDetail(String code, String message, List<FieldError> fieldErrors) {
    public ErrorDetail(String code, String message) {
        this(code, message, List.of());
    }
}
