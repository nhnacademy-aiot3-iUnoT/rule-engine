package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.engine.service.ZoneEnvStatusService;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZoneEnvStatusReportNodeTest {

    @Mock
    private ZoneEnvStatusService zoneEnvStatusService;

    private ZoneEnvStatusReportNode node;

    @BeforeEach
    void setUp() {
        node = new ZoneEnvStatusReportNode("zone-env-status", zoneEnvStatusService);
    }

    @Test
    @DisplayName("이벤트가 나온 구역의 상태 반영을 요청하고 다음 노드로 넘긴다")
    void reportZoneStatus() {
        // given
        LocalConnection connection = connect();

        when(zoneEnvStatusService.reportZoneStatus(1L, 2L, 3L))
                .thenReturn(Optional.of(EnvStatus.CRITICAL));

        // when
        node.process(messageWith(EnvStatus.CRITICAL));

        // then
        assertAll(
                () -> verify(zoneEnvStatusService).reportZoneStatus(1L, 2L, 3L),
                () -> assertEquals(1, connection.getBufferSize())
        );
    }

    @Test
    @DisplayName("구역 상태 반영이 실패해도 알림 노드로 메시지를 넘긴다")
    void continueWhenReportFails() {
        // given
        LocalConnection connection = connect();

        when(zoneEnvStatusService.reportZoneStatus(anyLong(), anyLong(), anyLong()))
                .thenThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "호출 실패"));

        // when
        assertDoesNotThrow(() -> node.process(messageWith(EnvStatus.WARNING)));

        // then
        assertEquals(1, connection.getBufferSize());
    }

    @Test
    @DisplayName("event가 없으면 구역 상태를 반영하지 않고 리턴한다")
    void stopWhenEventIsMissing() {
        // given
        LocalConnection connection = connect();

        // when
        assertDoesNotThrow(() -> node.process(new Message(Map.of())));

        // then
        assertAll(
                () -> verify(zoneEnvStatusService, never()).reportZoneStatus(any(), any(), any()),
                () -> assertEquals(0, connection.getBufferSize())
        );
    }

    private LocalConnection connect() {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        return connection;
    }

    private Message messageWith(EnvStatus currentStatus) {
        return new Message(Map.of(
                MessageFields.ENVIRONMENT_STATUS_EVENT,
                new EnvironmentStatusEventDto(
                        1L,
                        "device-eui",
                        2L,
                        3L,
                        "temperature",
                        ViolationType.ABOVE_MAX,
                        EnvStatus.NORMAL,
                        currentStatus,
                        EnvironmentEventReason.STATUS_CHANGED,
                        21.5,
                        0.0,
                        20.0,
                        "C",
                        "2026-08-12T00:00:00Z",
                        "temperature: 최댓값 초과"
                )
        ));
    }
}
