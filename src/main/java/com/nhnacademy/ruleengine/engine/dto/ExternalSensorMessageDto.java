package com.nhnacademy.ruleengine.engine.dto;

import com.nhnacademy.ruleengine.engine.MessageFields;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// 외부 MQTT payload에서 Rule Engine이 사용할 필드만 정리한 DTO다.
public record ExternalSensorMessageDto(
        String topic,
        Long receivedAt,
        String time,
        String applicationName,
        String devEui,
        String location,
        String point,
        Map<String, Object> measurements
) {

    public static ExternalSensorMessageDto from(Map<String, Object> payload) {
        // 중첩된 deviceInfo와 tags를 안전하게 꺼내 DTO로 변환한다.
        Map<String, Object> deviceInfo = mapValue(payload.get("deviceInfo"));
        Map<String, Object> tags = mapValue(deviceInfo.get("tags"));

        return new ExternalSensorMessageDto(
                stringValue(payload.get(MessageFields.TOPIC)),
                longValue(payload.get(MessageFields.MQTT_RECEIVED_AT)),
                stringValue(payload.get("time")),
                stringValue(deviceInfo.get("applicationName")),
                stringValue(deviceInfo.get("devEui")),
                stringValue(tags.get("location")),
                stringValue(tags.get("point")),
                mapValue(payload.get("object"))
        );
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapValue(Object value) {
        // Map이 아닌 값은 빈 Map으로 처리해 변환 흐름을 유지한다.
        if (value instanceof Map<?, ?> map) {
            return Collections.unmodifiableMap(new LinkedHashMap<>((Map<String, Object>) map));
        }
        return Map.of();
    }
    private static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }

        if (value instanceof String text && !text.isBlank()) {
            return Long.parseLong(text);
        }

        return null;
    }

}
