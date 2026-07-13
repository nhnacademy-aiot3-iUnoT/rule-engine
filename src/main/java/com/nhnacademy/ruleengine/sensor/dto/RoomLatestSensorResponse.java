package com.nhnacademy.ruleengine.sensor.dto;

import java.time.Instant;
import java.util.Map;

public record RoomLatestSensorResponse(
        String location,
        Map<String, SensorValueResponse> sensors
) {

    public record SensorValueResponse(
            double value,
            String unit,
            String deviceName,
            String deviceEui,
            Instant measuredAt
    ) {
    }
}
