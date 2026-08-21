package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentDecisionState;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneEnvStatusServiceTest {

    private static final String ZONE_KEY = "1:2:3";

    @Mock
    private EnvironmentDecisionStateRedisRepository decisionStateRepository;

    @Mock
    private InventoryClient inventoryClient;

    private ZoneEnvStatusService zoneEnvStatusService;

    @BeforeEach
    void setUp() {
        // 마지막으로 보낸 상태를 인스턴스가 기억하므로 테스트마다 새로 만든다.
        zoneEnvStatusService = new ZoneEnvStatusService(decisionStateRepository, inventoryClient);
    }

    @Test
    @DisplayName("구역 센서 중 가장 나쁜 상태를 인벤토리에 반영한다")
    void reportWorstStatusOfZone() {
        // given
        stubStates(Map.of(
                "device-eui:temperature", state(EnvStatus.CRITICAL),
                "device-eui:humidity", state(EnvStatus.WARNING),
                "device-eui:illumination", state(EnvStatus.NORMAL)
        ));

        // when
        Optional<EnvStatus> zoneStatus = zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        // then
        assertAll(
                () -> assertEquals(Optional.of(EnvStatus.CRITICAL), zoneStatus),
                () -> verify(inventoryClient).updateEnvStatus(3L, EnvStatus.CRITICAL)
        );
    }

    @Test
    @DisplayName("저장된 상태가 없으면 NORMAL을 반영한다")
    void reportNormalWhenNoState() {
        // given
        stubStates(Map.of());

        // when
        Optional<EnvStatus> zoneStatus = zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        // then
        assertAll(
                () -> assertEquals(Optional.of(EnvStatus.NORMAL), zoneStatus),
                () -> verify(inventoryClient).updateEnvStatus(3L, EnvStatus.NORMAL)
        );
    }

    @Test
    @DisplayName("직전에 보낸 상태와 같으면 다시 보내지 않는다")
    void skipWhenStatusUnchanged() {
        // given
        stubStates(Map.of("device-eui:temperature", state(EnvStatus.CRITICAL)));
        zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        // when
        Optional<EnvStatus> zoneStatus = zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        // then
        assertAll(
                () -> assertTrue(zoneStatus.isEmpty()),
                () -> verify(inventoryClient, times(1)).updateEnvStatus(3L, EnvStatus.CRITICAL)
        );
    }

    @Test
    @DisplayName("상태가 달라지면 다시 보낸다")
    void reportAgainWhenStatusChanged() {
        // given
        stubStates(Map.of("device-eui:temperature", state(EnvStatus.CRITICAL)));
        zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        stubStates(Map.of("device-eui:temperature", state(EnvStatus.NORMAL)));

        // when
        Optional<EnvStatus> zoneStatus = zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        // then
        assertAll(
                () -> assertEquals(Optional.of(EnvStatus.NORMAL), zoneStatus),
                () -> verify(inventoryClient).updateEnvStatus(3L, EnvStatus.NORMAL)
        );
    }

    @Test
    @DisplayName("전송에 실패하면 기억하지 않아 다음에 다시 시도한다")
    void retryAfterSendFailure() {
        // given
        stubStates(Map.of("device-eui:temperature", state(EnvStatus.CRITICAL)));

        doThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "호출 실패"))
                .doNothing()
                .when(inventoryClient).updateEnvStatus(3L, EnvStatus.CRITICAL);

        assertThrows(
                ApiException.class,
                () -> zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L)
        );

        // when
        Optional<EnvStatus> zoneStatus = zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L);

        // then
        assertAll(
                () -> assertEquals(Optional.of(EnvStatus.CRITICAL), zoneStatus),
                () -> verify(inventoryClient, times(2)).updateEnvStatus(3L, EnvStatus.CRITICAL)
        );
    }

    private void stubStates(Map<String, EnvironmentDecisionState> states) {
        when(decisionStateRepository.findSensorStatesByZone(ZONE_KEY)).thenReturn(states);
    }

    private EnvironmentDecisionState state(EnvStatus status) {
        return new EnvironmentDecisionState(
                status,
                null,
                null,
                Instant.now()
        );
    }
}
