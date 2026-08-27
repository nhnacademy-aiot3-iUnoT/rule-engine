package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SensorPayloadValidationNodeTest {

    private SensorPayloadValidationNode node;
    private LocalConnection connection;

    @BeforeEach
    void setUp() {
        node = new SensorPayloadValidationNode("validation");
        connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);
    }

    @Test
    @DisplayName("필수값이 모두 채워진 payload는 그대로 전달한다")
    void forwardValidPayload() throws InterruptedException {
        Message message = messageWith(sensorPayload());

        node.process(message);

        assertEquals(1, connection.getBufferSize());
        assertSame(message, connection.poll());
    }

    @Test
    @DisplayName("sensorPayload가 없으면 예외를 던진다")
    void rejectMissingPayload() {
        Message message = new Message(Map.of());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> node.process(message)
        );

        assertEquals("sensorPayload는 필수입니다.", exception.getMessage());
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("value가 없으면 예외를 던지고 전달하지 않는다")
    void rejectMissingValue() {
        Message message = messageWith(new SensorPayload(
                1L, "device-eui", 1L, 1L, "temperature", null, "C", "2026-08-12T00:00:00Z"
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> node.process(message)
        );

        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("organizationId가 양수가 아니면 예외를 던진다")
    void rejectInvalidOrganizationId() {
        Message message = messageWith(new SensorPayload(
                0L, "device-eui", 1L, 1L, "temperature", 21.5, "C", "2026-08-12T00:00:00Z"
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> node.process(message)
        );

        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("deviceEui가 공백이면 예외를 던진다")
    void rejectBlankDeviceEui() {
        Message message = messageWith(new SensorPayload(
                1L, "  ", 1L, 1L, "temperature", 21.5, "C", "2026-08-12T00:00:00Z"
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> node.process(message)
        );

        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("time이 없으면 예외를 던진다")
    void rejectMissingTime() {
        Message message = messageWith(new SensorPayload(
                1L, "device-eui", 1L, 1L, "temperature", 21.5, "C", null
        ));

        assertThrows(
                IllegalArgumentException.class,
                () -> node.process(message)
        );

        assertEquals(0, connection.getBufferSize());
    }

    private Message messageWith(SensorPayload sensorPayload) {
        return new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload));
    }

    private SensorPayload sensorPayload() {
        return new SensorPayload(
                1L,
                "device-eui",
                1L,
                1L,
                "temperature",
                21.5,
                "C",
                "2026-08-12T00:00:00Z"
        );
    }
}
