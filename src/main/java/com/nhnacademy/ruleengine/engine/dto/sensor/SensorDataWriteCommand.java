package com.nhnacademy.ruleengine.engine.dto.sensor;

import java.time.Instant;

public record SensorDataWriteCommand(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String sensorType,
        double value,
        String unit,
        Instant timestamp
) {
    public static SensorDataWriteCommand of(
            Long organizationId,
            String deviceEui,
            Long storageId,
            Long sectionId,
            String sensorType,
            double value,
            String unit,
            Instant timestamp
    ){
        return new SensorDataWriteCommand(
                organizationId,
                deviceEui,
                storageId,
                sectionId,
                sensorType,
                value,
                unit,
                timestamp);
    }
}