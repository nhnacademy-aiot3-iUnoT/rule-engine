package com.nhnacademy.ruleengine.engine.dto.notification;

// 알림 옵션 설정
public record NotificationPreference(
        Long userId,
        Long organizationId,
        Long storageId,
        Long zoneId,
        NotificationChannel channel,
        boolean enabled,
        String recipient
        // recipient는 채널별 수신자 식별자다.
        // TELEGRAM: chat_id

        //Map<String, Object> config - 채널 별 추가 설정
){
}
