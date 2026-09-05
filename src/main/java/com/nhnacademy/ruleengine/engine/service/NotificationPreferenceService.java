package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.global.client.InventoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 알림 수신자 조회.
 * 메신저 수신자는 조직원이 직접 설정한 값이라 인벤토리에 물어보고,
 * 웹 알림은 개인 설정이 아니라 환경 이벤트 기록이므로 항상 한 건 포함한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final InventoryClient inventoryClient;

    public List<NotificationPreference> findPreferences(
            Long organizationId,
            Long storageId,
            Long zoneId
    ) {
        List<NotificationPreference> preferences = new ArrayList<>();

        // 웹 알림(환경 이벤트 기록)은 수신자와 무관하게 남긴다.
        preferences.add(new NotificationPreference(
                null, organizationId, storageId, zoneId, NotificationChannel.WEB, true, null
        ));

        try {
            preferences.addAll(inventoryClient.findNotificationReceivers(organizationId, storageId, zoneId));
        } catch (Exception e) {
            // 수신자 조회가 실패해도 환경 이벤트 기록은 남기고 넘어간다.
            log.warn("알림 수신자 조회 실패. organizationId={}, storageId={}, zoneId={}",
                    organizationId, storageId, zoneId, e);
        }

        return preferences;
    }
}
