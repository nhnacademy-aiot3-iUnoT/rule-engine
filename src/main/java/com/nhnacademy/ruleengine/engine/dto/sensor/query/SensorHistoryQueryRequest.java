package com.nhnacademy.ruleengine.engine.dto.sensor.query;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.Instant;

public record SensorHistoryQueryRequest(
        String sensorType,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        Instant to,

        String window
) {
}
