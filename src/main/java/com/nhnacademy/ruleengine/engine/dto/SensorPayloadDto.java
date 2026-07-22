package com.nhnacademy.ruleengine.engine.dto;

// 내부 MQTT로 발행할 표준 센서 데이터 형식이다.
public record SensorPayloadDto(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String sensorType,
        Double value,
        String unit,
        String time
) {

    public static SensorPayloadDto fromSensor(
            Long organizationId,
            String deviceEui,
            Long storageId,
            Long sectionId,
            String sensorType,
            Double value,
            String unit,
            String time
    ) {
        // 센서 변환 결과를 표준 payload로 생성한다.
        return new SensorPayloadDto(
                organizationId,
                deviceEui,
                storageId,
                sectionId,
                sensorType,
                value,
                unit,
                time);
    }

}
