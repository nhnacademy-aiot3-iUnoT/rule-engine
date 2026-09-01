package com.nhnacademy.ruleengine.engine.notification.impl;

import com.nhnacademy.ruleengine.engine.dto.notification.EnvironmentEventCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
import com.nhnacademy.ruleengine.global.client.InventoryClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 웹 알림은 인벤토리의 환경 이벤트로 저장된다.
@Slf4j
@Component
@RequiredArgsConstructor
public class WebNotificationSender implements NotificationSender {

    private final InventoryClient inventoryClient;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.WEB;
    }

    @Override
    public void send(NotificationRequest request, NotificationPreference preference) {
        EnvironmentEventCreateRequest.from(request).ifPresentOrElse(
                inventoryClient::createEnvironmentEvent,
                () -> log.info(
                        "[{}] 환경 이벤트로 만들 수 없어 발송을 건너뜁니다. zoneId={}, sensorType={}, violationType={}, value={}",
                        channel(),
                        request.zoneId(),
                        request.sensorType(),
                        request.violationType(),
                        request.value()
                )
        );
    }
}
