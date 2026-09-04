package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.global.client.InventoryClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {
    @Mock
    InventoryClient inventoryClient;

    @InjectMocks
    NotificationPreferenceService notificationPreferenceService;

    @Test
    @DisplayName("inventory client에서 알림 설정 목록을 조회한다")
    void findPreferences() {
        List<NotificationPreference> expected = List.of(
                new NotificationPreference(
                        25L,
                        19L,
                        6L,
                        43L,
                        NotificationChannel.TELEGRAM,
                        true,
                        "7501086554"
                )
        );

        when(inventoryClient.findNotificationPreferences(19L, 6L, 43L))
                .thenReturn(expected);

        List<NotificationPreference> result =
                notificationPreferenceService.findPreferences(19L, 6L, 43L);

        assertEquals(expected, result);
        verify(inventoryClient).findNotificationPreferences(19L, 6L, 43L);
    }


    @Test
    @DisplayName("inventory client 조회 결과가 비어 있으면 빈 리스트를 반환한다")
    void findPreferencesReturnsEmptyList() {
        when(inventoryClient.findNotificationPreferences(19L, 6L, 43L))
                .thenReturn(List.of());

        List<NotificationPreference> result =
                notificationPreferenceService.findPreferences(19L, 6L, 43L);

        assertEquals(List.of(), result);
        verify(inventoryClient).findNotificationPreferences(19L, 6L, 43L);
    }
}