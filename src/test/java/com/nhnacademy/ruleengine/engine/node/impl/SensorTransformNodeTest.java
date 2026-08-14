package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.SensorTransformService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SensorTransformNodeTest {

    @Mock
    private SensorTransformService sensorTransformService;

    private SensorTransformNode sensorTransformNode;

    @BeforeEach
    void setUp() {
        sensorTransformNode = new SensorTransformNode("transform", sensorTransformService);
    }

    @Test
    @DisplayName("외부 센서데이터를 센서 타입별 페이로드로 변환")
    void sendOneMessagePerPayload() throws InterruptedException {
        LocalConnection connection = connect(sensorTransformNode);

        SensorPayload temperature = sensorPayload("temperature", 21.5);
        SensorPayload humidity = sensorPayload("humidity", 55.0);

        when(sensorTransformService.transform(any()))
                .thenReturn(List.of(temperature, humidity));

        sensorTransformNode.process(messageWith(externalSensorMessage()));

        assertEquals(2, connection.getBufferSize());
        assertEquals(temperature, connection.poll().get(MessageFields.SENSOR_PAYLOAD));
        assertEquals(humidity, connection.poll().get(MessageFields.SENSOR_PAYLOAD));

    }

    @Test
    @DisplayName("변환 결과가 비어 있을 때 메시지를 보내지 않음")
    void doNotSendWhenTransformResultIsEmpty() {
        LocalConnection connection = connect(sensorTransformNode);

        sensorTransformNode.process(new Message(Map.of()));

        assertEquals(0, connection.getBufferSize());
        verify(sensorTransformService, never()).transform(any());
    }

    private LocalConnection connect(AbstractNode node) {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        return connection;
    }

    private Message messageWith(ExternalSensorMessage externalSensorMessage) {
        return new Message(Map.of(
                MessageFields.EXTERNAL_SENSOR_MESSAGE, externalSensorMessage
        ));
    }

    private ExternalSensorMessage externalSensorMessage() {
        return new ExternalSensorMessage(
                "application/1/device/device-eui/event/up",
                1L,
                "2026-08-12T00:00:00Z",
                "application",
                "device-eui",
                "storage-1",
                "zone-1",
                Map.of("temperature", 21.5, "humidity", 55.0)
        );
    }

    private SensorPayload sensorPayload(String sensorType, Double value) {
        return new SensorPayload(
                1L,
                "device-eui",
                1L,
                1L,
                sensorType,
                value,
                "celsius",
                "2026-08-12T00:00:00Z"
        );
    }
}