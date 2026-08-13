package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.FlowProcessingCompletion;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseSaveNodeTest {

    @Mock
    private SensorInfluxService influxService;
    private DatabaseSaveNode node;

    @BeforeEach
    void setUp() {
        node = new DatabaseSaveNode("db-save", influxService);
    }

    @Test
    @DisplayName("sensorPayload를 InfluxDB에 저장한다")
    void saveSensorPayload() {
        // given
        SensorPayload sensorPayload = sensorPayload();
        Message message = messageWith(sensorPayload);

        // when
        node.process(message);

        // then
        verify(influxService).save(sensorPayload);
    }

    @Test
    @DisplayName("저장 후 out 포트로 메시지를 그대로 전달한다")
    void sendToOutputPort() throws Exception {
        // given
        LocalConnection connection = sensorConnection();
        node.getOutputPort("out").connect(connection);

        Message message = messageWith(sensorPayload());

        // when
        node.process(message);

        // then
        assertEquals(1, connection.getBufferSize());
        assertSame(message, connection.poll());
    }

    @Test
    @DisplayName("sensorPayload가 없으면 IllegalArgumentException이 발생한다")
    void sensorPayloadIsRequired() {
        // given
        Message message = new Message(new HashMap<>());

        // when
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> node.process(message)
        );

        // then
        assertEquals("sensorPayload는 필수입니다.", exception.getMessage());
        verify(influxService, never()).save(any());
    }

    @Test
    @DisplayName("저장에 실패하면 예외를 전파하고 다음 노드로 전달하지 않는다")
    void doNotSendWhenSaveFails() {
        // given
        LocalConnection connection = sensorConnection();
        node.getOutputPort("out").connect(connection);

        doThrow(new IllegalStateException("influx down"))
                .when(influxService)
                .save(any());

        Message message = messageWith(sensorPayload());

        // when
        assertThrows(
                IllegalStateException.class,
                () -> node.process(message)
        );

        // then
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("out 포트에 연결이 없으면 이 노드에서 처리를 완료한다")
    void completeProcessingWhenNotConnected() {
        // given
        FlowProcessingCompletion completion = new FlowProcessingCompletion();

        Message message = new Message(
                Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload()),
                completion
        );

        // when
        node.process(message);

        // then
        assertDoesNotThrow(() -> completion.await(Duration.ofMillis(100)));
    }

    private LocalConnection sensorConnection() {
        return new LocalConnection("sensor-connection");
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
                "celsius",
                "2026-08-12T00:00:00Z"
        );
    }
}
