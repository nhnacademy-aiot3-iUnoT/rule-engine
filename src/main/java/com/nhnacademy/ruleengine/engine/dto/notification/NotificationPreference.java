package com.nhnacademy.ruleengine.engine.dto.notification;

// 알림 옵션 설정
public record NotificationPreference(
        Long userId,
        Long organizationId,
        Long storageId,
        Long sectionId,
        NotificationChannel channel,
        boolean enabled,
        String recipient
        //Map<String, Object> config - 채널 별 추가 설정
){
}
