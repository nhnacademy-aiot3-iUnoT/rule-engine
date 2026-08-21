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
            Long zoneId
    ) {
        return List.of(
                new NotificationPreference(1L, organizationId, storageId, zoneId, NotificationChannel.WEB, true, null),
                new NotificationPreference(1L, organizationId, storageId, zoneId, NotificationChannel.TELEGRAM, true, "7501086554"),
                new NotificationPreference(1L, organizationId, storageId, zoneId, NotificationChannel.KAKAO, false, "test-kakao-receiver-id")
        );
    }

    //List<NotificationPreference> findByUserId(Long userId);
}
