package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.global.client.InventoryClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final InventoryClient inventoryClient;

    public List<NotificationPreference> findPreferences(
            Long organizationId,
            Long storageId,
            Long zoneId
    ) {
        return inventoryClient.findNotificationPreferences(organizationId, storageId, zoneId);
    }
}
