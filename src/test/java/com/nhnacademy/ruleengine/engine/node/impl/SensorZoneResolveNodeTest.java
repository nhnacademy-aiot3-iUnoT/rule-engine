package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.service.ZoneResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SensorZoneResolveNodeTest {

    private static final String DEVICE_EUI = "device eui";

    @Mock
    private ZoneResolver zoneResolver;

    private SensorZoneResolveNode node;
    private LocalConnection connection;

    @BeforeEach
    void setUp() {
        node = new SensorZoneResolveNode("zone-resolve", zoneResolver);

        connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);
    }

    @Test
    @DisplayName("구역에 등록된 기기면 위치를 채워서 넘긴다")
    void fillsLocation() throws InterruptedException {
        when(zoneResolver.resolveActive(DEVICE_EUI))
                .thenReturn(Optional.of(new ResolvedZoneResponse(1L, 2L, 3L)));

        node.process(message());

        Message resolved = connection.poll();
        SensorPayload payload = resolved.get(MessageFields.SENSOR_PAYLOAD);

        assertAll(
                () -> assertEquals(1L, payload.organizationId()),
                () -> assertEquals(2L, payload.storageId()),
                () -> assertEquals(3L, payload.zoneId()),
                () -> assertEquals(DEVICE_EUI, payload.deviceEui()),
                () -> assertEquals(21.5, payload.value()),
                () -> assertEquals("1/2/3/device_eui/temperature", resolved.get(MessageFields.TOPIC))
        );
    }

    @Test
    @DisplayName("구역에 등록되지 않은 기기의 데이터는 버린다")
    void dropsUnregisteredDevice() {
        when(zoneResolver.resolveActive(DEVICE_EUI)).thenReturn(Optional.empty());

        node.process(message());

        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("센서 데이터가 없으면 아무것도 넘기지 않는다")
    void dropsEmptyMessage() {
        node.process(new Message(Map.of()));

        assertEquals(0, connection.getBufferSize());
    }

    private Message message() {
        return new Message(Map.of(
                MessageFields.SENSOR_PAYLOAD,
                new SensorPayload(
                        null,
                        DEVICE_EUI,
                        null,
                        null,
                        "temperature",
                        21.5,
                        "C",
                        "2026-08-27T00:00:00Z"
                )
        ));
    }
}
