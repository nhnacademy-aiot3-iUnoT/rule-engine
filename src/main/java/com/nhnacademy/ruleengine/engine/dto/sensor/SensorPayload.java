package com.nhnacademy.ruleengine.engine.dto.sensor;

// 내부 MQTT로 발행할 표준 센서 데이터 형식이다.
public record SensorPayload(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long zoneId,
        String sensorType,
        Double value,
        String unit,
        String time
) {
}
