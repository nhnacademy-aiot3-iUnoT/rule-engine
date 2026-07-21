package com.nhnacademy.ruleengine.engine.dto;

import jakarta.validation.constraints.NotNull;

public record SensorRangeRequest(

        @NotNull(message = "최솟값은 필수입니다.")
        Double min,

        @NotNull(message = "최댓값은 필수입니다.")
        Double max

) {
    public SensorRangeRequest {
        if (min != null && max != null && min > max) {
            throw new IllegalArgumentException(
                    "최솟값은 최댓값보다 클 수 없습니다."
            );
        }
    }
}