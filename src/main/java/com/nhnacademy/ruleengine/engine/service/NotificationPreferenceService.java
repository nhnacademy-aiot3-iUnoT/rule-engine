package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.NotificationPreference;

import java.util.List;

public class NotificationPreferenceService {
    public List<NotificationPreference> findPreferences(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        return List.of(
                new NotificationPreference(1L, organizationId, storageId, sectionId, NotificationChannel.WEB, true, null),
                new NotificationPreference(1L, organizationId, storageId, sectionId, NotificationChannel.TELEGRAM, true, "test-telegram-chat-id"),
                new NotificationPreference(1L, organizationId, storageId, sectionId, NotificationChannel.KAKAO, false, "test-kakao-receiver-id")
        );
    }

    //List<NotificationPreference> findByUserId(Long userId);
}
