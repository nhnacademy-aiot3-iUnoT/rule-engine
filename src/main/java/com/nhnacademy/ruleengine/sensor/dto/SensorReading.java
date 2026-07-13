package com.nhnacademy.ruleengine.sensor.dto;

import java.time.Instant;

// InfluxDB 조회 결과를 서비스 계층으로 전달하는 센서 측정값이다.
public record SensorReading(
        String sensorType,
        double value,
        String unit,
        String deviceName,
        String deviceEui,
        Instant measuredAt
) {
}
