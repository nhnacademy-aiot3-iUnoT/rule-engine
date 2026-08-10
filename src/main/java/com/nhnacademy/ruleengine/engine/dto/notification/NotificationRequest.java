package com.nhnacademy.ruleengine.engine.dto.notification;

import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;

import java.util.Map;

import static com.nhnacademy.ruleengine.engine.notification.NotificationMessageFormatter.*;

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
}
