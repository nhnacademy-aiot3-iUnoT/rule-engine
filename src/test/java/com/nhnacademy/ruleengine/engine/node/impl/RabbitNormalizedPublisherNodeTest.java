package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RabbitNormalizedPublisherNodeTest {

    @Mock
    private NormalizedSensorPublisher normalizedSensorPublisher;

    private RabbitNormalizedPublisherNode node;

    @BeforeEach
    void setUp() {
        node = new RabbitNormalizedPublisherNode("publisher", normalizedSensorPublisher);
    }

    @Test
    @DisplayName("sensorPayload가 있으면 그대로 발행한다")
    void publishSensorPayload() {
        SensorPayload sensorPayload = sensorPayload();

        node.process(messageWith(sensorPayload));

        verify(normalizedSensorPublisher).publish(sensorPayload);
    }

    @Test
    @DisplayName("sensorPayload가 없으면 발행하지 않는다")
    void doNotPublishWithoutSensorPayload() {
        node.process(new Message(Map.of()));

        verify(normalizedSensorPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("발행 실패 예외는 그대로 전파해 요청자가 재시도를 판단하게 한다")
    void propagatePublishFailure() {
        doThrow(new IllegalArgumentException("표준 센서 데이터 직렬화 실패"))
                .when(normalizedSensorPublisher).publish(any());

        assertThrows(
                IllegalArgumentException.class,
                () -> node.process(messageWith(sensorPayload()))
        );
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
