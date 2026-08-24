package com.nhnacademy.ruleengine.engine.node.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class NormalizedSensorConsumerNodeTest {

    private static final String VALID_PAYLOAD = """
            {"organizationId":1,"deviceEui":"device-eui","storageId":1,"zoneId":1,
             "sensorType":"temperature","value":21.5,"unit":"C","time":"2026-08-12T00:00:00Z"}
            """;

    private NormalizedSensorConsumerNode node;

    @BeforeEach
    void setUp() {
        node = new NormalizedSensorConsumerNode(new ObjectMapper());
    }

    @Test
    @DisplayName("역직렬화한 센서 데이터를 다음 노드로 전달한다")
    void consumeValidPayload() {
        List<Message> delivered = connectDownstream(Message::completeProcessing);

        node.consume(VALID_PAYLOAD);

        assertEquals(1, delivered.size());

        SensorPayload sensorPayload = delivered.get(0).get(MessageFields.SENSOR_PAYLOAD);

        assertAll(
                () -> assertEquals(1L, sensorPayload.organizationId()),
                () -> assertEquals("device-eui", sensorPayload.deviceEui()),
                () -> assertEquals("temperature", sensorPayload.sensorType()),
                () -> assertEquals(21.5, sensorPayload.value()),
                () -> assertEquals("2026-08-12T00:00:00Z", sensorPayload.time())
        );
    }

    @Test
    @DisplayName("역직렬화에 실패한 payload는 예외 없이 버린다")
    void skipBrokenPayload() {
        List<Message> delivered = connectDownstream(Message::completeProcessing);

        assertDoesNotThrow(() -> node.consume("not a json"));
        assertTrue(delivered.isEmpty());
    }

    @Test
    @DisplayName("다음 노드가 없으면 Flow 완료로 처리해 대기하지 않는다")
    void completeWhenNoDownstream() {
        assertDoesNotThrow(() -> node.consume(VALID_PAYLOAD));
    }

    @Test
    @DisplayName("Flow가 검증 실패로 끝나면 재큐잉하지 않는 예외로 바꾼다")
    void rejectOnValidationFailure() {
        connectDownstream(message -> message.completeProcessingExceptionally(
                new IllegalArgumentException("sensorType는 필수입니다.")
        ));

        AmqpRejectAndDontRequeueException exception = assertThrows(
                AmqpRejectAndDontRequeueException.class,
                () -> node.consume(VALID_PAYLOAD)
        );

        assertInstanceOf(IllegalArgumentException.class, exception.getCause());
    }

    @Test
    @DisplayName("검증 외의 Flow 실패는 재큐잉 가능한 예외로 바꾼다")
    void wrapOtherFlowFailure() {
        connectDownstream(message -> message.completeProcessingExceptionally(
                new IllegalStateException("DB 연결 실패")
        ));

        AmqpException exception = assertThrows(
                AmqpException.class,
                () -> node.consume(VALID_PAYLOAD)
        );

        assertFalse(exception instanceof AmqpRejectAndDontRequeueException);
        assertInstanceOf(IllegalStateException.class, exception.getCause());
    }

    @Test
    @DisplayName("onProcess는 입력 메시지를 그대로 전달한다")
    void onProcessForwardsMessage() throws InterruptedException {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        Message message = new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload()));
        node.process(message);

        assertEquals(1, connection.getBufferSize());
        assertSame(message, connection.poll());
    }

    // consume()은 Flow 완료를 기다리므로, 하위 노드 대신 완료 통지까지 흉내내는 Connection을 붙인다.
    private List<Message> connectDownstream(Consumer<Message> onDeliver) {
        List<Message> delivered = new ArrayList<>();

        node.getOutputPort("out").connect(new LocalConnection("out-connection") {
            @Override
            public void deliver(Message message) {
                delivered.add(message);
                onDeliver.accept(message);
            }
        });

        return delivered;
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
