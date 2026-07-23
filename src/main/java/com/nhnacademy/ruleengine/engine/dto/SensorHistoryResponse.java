package com.nhnacademy.ruleengine.engine.dto;

import java.time.Instant;

public record SensorHistoryResponse(
        String sensorType,
        String unit,
        Instant time,
        Double value
) {
}
