package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record VirtualSensorUpdateRequest(

        @Min(value = 1, message = "측정 주기는 1초 이상이어야 합니다.")
        Long measurementIntervalSeconds,

        @NotEmpty(message = "센서 타입을 하나 이상 선택해야 합니다.")
        Set<SensorType> enabledSensorTypes,


        @Valid
        SensorValueRange temperature,

        @Valid
        SensorValueRange humidity,

        @Valid
        SensorValueRange illumination,

        @DecimalMin(value = "0.0", message = "문 열림 확률은 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "문 열림 확률은 1 이하여야 합니다.")
        Double doorOpenProbability
) {
}