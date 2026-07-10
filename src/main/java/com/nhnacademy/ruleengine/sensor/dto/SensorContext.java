package com.nhnacademy.ruleengine.sensor.dto;

import com.nhnacademy.ruleengine.mqtt.dto.MqttInboundMessageDto;

// 센서별 변환 로직에서 공통으로 사용하는 장치 정보를 담는다.
public record SensorContext(
        String applicationName,
        String deviceName,
        String deviceEui,
        String location,
        String time
) {

    public static SensorContext from(
            MqttInboundMessageDto mqttInbound
    ) {
        // 누락된 장치 정보는 일관되게 unknown으로 보정한다.
        return new SensorContext(
                valueOrUnknown(mqttInbound.applicationName()),
                valueOrUnknown(mqttInbound.deviceName()),
                valueOrUnknown(mqttInbound.devEui()),
                valueOrUnknown(mqttInbound.location()),
                valueOrUnknown(mqttInbound.time())
        );
    }

    private static String valueOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}
