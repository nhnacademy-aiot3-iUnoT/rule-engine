package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
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
