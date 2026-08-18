package com.nhnacademy.ruleengine.engine.dto.virtual;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

public record VirtualSensorCreateRequest(

        @NotBlank(message = "deviceEui는 필수입니다.")
        String deviceEui,

        @NotNull(message = "측정 주기는 필수입니다.")
        @Min(value = 1, message = "측정 주기는 1초 이상이어야 합니다.")
        Integer measurementIntervalSeconds,

        @Valid
        SensorValueRange temperature,

        @Valid
        SensorValueRange humidity,

        @Valid
        SensorValueRange illumination,

        @NotNull(message = "문 열림 확률은 필수입니다.")
        @DecimalMin(value = "0.0", message = "문 열림 확률은 0 이상이어야 합니다.")
        @DecimalMax(value = "1.0", message = "문 열림 확률은 1 이하여야 합니다.")
        Double doorOpenProbability
) {
}