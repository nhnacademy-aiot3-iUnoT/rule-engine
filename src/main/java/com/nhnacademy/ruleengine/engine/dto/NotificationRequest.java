package com.nhnacademy.ruleengine.engine.dto;

import java.util.HashMap;
import java.util.Map;

// 알림 채널별 전송에 필요한 공통 정보와 이벤트 원본 정보를 담음
public record NotificationRequest(
        Long organizationId,
        Long storageId,
        Long sectionId,
        String deviceEui,
        String sensorType,

        EnvironmentStatus previousStatus,
        EnvironmentStatus currentStatus,
        EnvironmentEventReason reason,

        String title,
        String content,
        String occurredAt,
        Map<String, Object> metadata
) {
    public static NotificationRequest from(EnvironmentStatusEventDto event){
        return new NotificationRequest(
                event.organizationId(),
                event.storageId(),
                event.sectionId(),
                event.deviceEui(),
                event.sensorType(),
                event.previousStatus(),
                event.currentStatus(),
                event.reason(),
                titleOf(event),
                contentOf(event),
                event.measuredAt(),
                metadataOf(event)
        );
    }

    private static String titleOf(EnvironmentStatusEventDto event){
        return switch (event.currentStatus()){
            case NORMAL -> "환경 상태 정상 복귀";
            case WARNING -> "환경 상태 경고";
            case CRITICAL -> "환경 상태 위험";
        };
    }

    private static String contentOf(EnvironmentStatusEventDto event){
        return String.format(
                "%s 센서가 %s 상태입니다. 현재값: %.2f%s, 사유: %s",
                event.sensorType(),
                event.currentStatus(),
                event.value(),
                event.unit(),
                event.message()
        );
    }

    private static  Map<String, Object> metadataOf(EnvironmentStatusEventDto event){
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("value", event.value());
        metadata.put("unit", event.unit());
        metadata.put("message", event.message());
        return metadata;
    }
}
