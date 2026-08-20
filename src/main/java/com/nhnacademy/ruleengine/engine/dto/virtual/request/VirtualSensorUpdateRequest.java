package com.nhnacademy.ruleengine.engine.dto.virtual.request;

import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;
import jakarta.validation.constraints.*;


public record VirtualSensorUpdateRequest(

        @Min(value = 1, message = "측정 주기는 1초 이상이어야 합니다.")
        Long measurementIntervalSeconds,

        @NotNull
        VirtualSensorValues virtualSensorValues
) {
}