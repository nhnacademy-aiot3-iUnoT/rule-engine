package com.nhnacademy.ruleengine.engine.dto.virtual.request;

import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import jakarta.validation.constraints.NotNull;

public record VirtualSensorStatusRequest(

        @NotNull(message = "가상 센서 상태는 필수입니다.")
        VirtualSensorStatus status
) {
}