package com.nhnacademy.ruleengine.engine.notification.impl;

import com.nhnacademy.ruleengine.engine.domain.EnvironmentType;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.notification.EnvironmentEventCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.global.client.InventoryClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebNotificationSenderTest {

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private WebNotificationSender sender;

    @Test
    @DisplayName("channel은 WEB을 반환한다")
    void channel() {
        assertEquals(NotificationChannel.WEB, sender.channel());
    }

    @Test
    @DisplayName("최댓값 초과 이벤트는 최댓값을 임계값으로 보낸다")
    void aboveMax() {
        sender.send(request("temperature", ViolationType.ABOVE_MAX, EnvStatus.CRITICAL, 30.0, 18.0, 26.0), preference());

        EnvironmentEventCreateRequest captured = capturedRequest();

        assertAll(
                () -> assertEquals(3L, captured.zoneId()),
                () -> assertEquals(BigDecimal.valueOf(30.0), captured.detectedValue()),
                () -> assertEquals(BigDecimal.valueOf(26.0), captured.thresholdValue()),
                () -> assertEquals(EnvironmentType.TEMPERATURE, captured.environmentType()),
                () -> assertEquals(ViolationType.ABOVE_MAX, captured.breachType())
        );
    }

    @Test
    @DisplayName("최솟값 미달 이벤트는 최솟값을 임계값으로 보낸다")
    void belowMin() {
        sender.send(request("humidity", ViolationType.BELOW_MIN, EnvStatus.CRITICAL, 10.0, 30.0, 70.0), preference());

        EnvironmentEventCreateRequest captured = capturedRequest();

        assertAll(
                () -> assertEquals(BigDecimal.valueOf(30.0), captured.thresholdValue()),
                () -> assertEquals(EnvironmentType.HUMIDITY, captured.environmentType()),
                () -> assertEquals(ViolationType.BELOW_MIN, captured.breachType())
        );
    }

    @Test
    @DisplayName("illumination은 인벤토리의 ILLUMINANCE로 보낸다")
    void illumination() {
        sender.send(request("illumination", ViolationType.ABOVE_MAX, EnvStatus.CRITICAL, 900.0, 100.0, 800.0), preference());

        assertEquals(EnvironmentType.ILLUMINANCE, capturedRequest().environmentType());
    }

    @Test
    @DisplayName("정상 복귀 이벤트는 NORMAL 위반유형으로 발송한다")
    void normalRecovery() {
        sender.send(request("temperature", ViolationType.NORMAL, EnvStatus.NORMAL, 22.0, 18.0, 26.0), preference());

        EnvironmentEventCreateRequest captured = capturedRequest();

        assertAll(
                () -> assertEquals(ViolationType.NORMAL, captured.breachType()),
                () -> assertEquals(BigDecimal.valueOf(22.0), captured.detectedValue()),
                () -> assertEquals(BigDecimal.valueOf(26.0), captured.thresholdValue())
        );
    }

    @Test
    @DisplayName("문 센서 이벤트는 DOOR 환경유형으로 발송한다")
    void door() {
        sender.send(request("door", ViolationType.OPEN, EnvStatus.CRITICAL, 1.0, null, null), preference());

        EnvironmentEventCreateRequest captured = capturedRequest();

        assertAll(
                () -> assertEquals(EnvironmentType.DOOR, captured.environmentType()),
                () -> assertEquals(ViolationType.OPEN, captured.breachType()),
                () -> assertEquals(BigDecimal.valueOf(1.0), captured.detectedValue()),
                // 문은 임계값 개념이 없어 닫힘(0)을 기준값으로 보낸다.
                () -> assertEquals(BigDecimal.ZERO, captured.thresholdValue())
        );
    }

    @Test
    @DisplayName("지원하지 않는 센서타입은 발송하지 않는다")
    void unknownSensorTypeIsSkipped() {
        sender.send(request("co2", ViolationType.ABOVE_MAX, EnvStatus.CRITICAL, 900.0, 100.0, 800.0), preference());

        verify(inventoryClient, never()).createEnvironmentEvent(any());
    }

    @Test
    @DisplayName("임계값이 없으면 발송하지 않는다")
    void missingThresholdIsSkipped() {
        sender.send(request("temperature", ViolationType.ABOVE_MAX, EnvStatus.CRITICAL, 30.0, 18.0, null), preference());

        verify(inventoryClient, never()).createEnvironmentEvent(any());
    }

    private EnvironmentEventCreateRequest capturedRequest() {
        ArgumentCaptor<EnvironmentEventCreateRequest> captor =
                ArgumentCaptor.forClass(EnvironmentEventCreateRequest.class);
        verify(inventoryClient).createEnvironmentEvent(captor.capture());
        return captor.getValue();
    }

    private NotificationRequest request(
            String sensorType,
            ViolationType violationType,
            EnvStatus currentStatus,
            Double value,
            Double min,
            Double max
    ) {
        return new NotificationRequest(
                1L,
                2L,
                3L,
                "device-1",
                sensorType,
                EnvStatus.NORMAL,
                currentStatus,
                EnvironmentEventReason.STATUS_CHANGED,
                violationType,
                value,
                min,
                max,
                "제목",
                "내용",
                "2026-08-12T10:00:00Z",
                Map.of()
        );
    }

    private NotificationPreference preference() {
        return new NotificationPreference(1L, 1L, 2L, 3L, NotificationChannel.WEB, true, null);
    }
}
