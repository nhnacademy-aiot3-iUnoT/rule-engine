package com.nhnacademy.ruleengine.engine.dto.virtual;

import jakarta.validation.constraints.NotNull;

public record VirtualSensorStatusRequest(

        @NotNull(message = "가상 센서 상태는 필수입니다.")
        VirtualSensorStatus status
) {
}