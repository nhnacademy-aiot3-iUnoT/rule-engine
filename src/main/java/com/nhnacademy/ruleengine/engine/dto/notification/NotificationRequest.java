package com.nhnacademy.ruleengine.engine.dto.notification;

import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;

import java.util.Map;

import static com.nhnacademy.ruleengine.engine.notification.NotificationMessageFormatter.*;

// 알림 채널별 전송에 필요한 공통 정보와 이벤트 원본 정보를 담음
// title/content는 메시지형 채널(텔레그램, 카카오)이 쓰고,
// violationType/value/min/max는 측정값 그대로를 요구하는 채널(웹)이 쓴다.
public record NotificationRequest(
        Long organizationId,
        Long storageId,
        Long zoneId,
        String deviceEui,
        String sensorType,

        EnvStatus previousStatus,
        EnvStatus currentStatus,
        EnvironmentEventReason reason,

        ViolationType violationType,
        Double value,
        Double min,
        Double max,

        String title,
        String content,
        String occurredAt,
        Map<String, Object> metadata
) {
    public static NotificationRequest from(EnvironmentStatusEventDto event){
        return new NotificationRequest(
                event.organizationId(),
                event.storageId(),
                event.zoneId(),
                event.deviceEui(),
                event.sensorType(),
                event.previousStatus(),
                event.currentStatus(),
                event.reason(),
                event.violationType(),
                event.value(),
                event.min(),
                event.max(),
                titleOf(event),
                contentOf(event),
                event.measuredAt(),
                metadataOf(event)
        );
    }
}
