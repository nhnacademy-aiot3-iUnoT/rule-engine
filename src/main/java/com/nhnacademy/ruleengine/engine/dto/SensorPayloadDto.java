package com.nhnacademy.ruleengine.engine.dto;

// 내부 MQTT로 발행할 표준 센서 데이터 형식이다.
public record SensorPayloadDto(
        String applicationName,
        String deviceName,
        String deviceEui,
        String location,
        Long locationId,
        String sensorType,
        Double value,
        String unit,
        String time
) {

    public static SensorPayloadDto fromSensor(
            String applicationName,
            String deviceName,
            String deviceEui,
            String location,
            String sensorType,
            double value,
            String unit,
            String time
    ) {
        // 센서 변환 결과를 표준 payload로 생성한다.
        return new SensorPayloadDto(
                applicationName,
                deviceName,
                deviceEui,
                location,
                null,
                sensorType,
                value,
                unit,
                time);
    }

    public SensorPayloadDto withLocationId(long resolvedLocationId) {
        if (resolvedLocationId <= 0) {
            throw new IllegalArgumentException("locationId는 양수여야 합니다.");
        }

        return new SensorPayloadDto(
                applicationName,
                deviceName,
                deviceEui,
                location,
                resolvedLocationId,
                sensorType,
                value,
                unit,
                time
        );
    }
}
