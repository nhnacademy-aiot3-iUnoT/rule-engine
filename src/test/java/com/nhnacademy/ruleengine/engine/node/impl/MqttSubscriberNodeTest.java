package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.exception.ConnectionException;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MqttSubscriberNodeTest {

    private static final String TOPIC_NAME = "sensor/temperature";
    private static final String UNREACHABLE_BROKER = "tcp://127.0.0.1:1";

    private MqttSubscriberNode createNode() {
        return new MqttSubscriberNode("test", Map.of());
    }

    private byte[] bytesOf(String payload) {
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("정상 JSON payload는 Map으로 파싱되고 topic과 수신시각이 추가된다")
    void parseValidPayload() {
        MqttSubscriberNode node = createNode();
        String json = """
                {"deviceInfo":{"devEui":"abc123","applicationName":"app"},"object":{"temperature":25.5}}
                """;

        Map<String, Object> parsed = node.parsePayload(TOPIC_NAME, bytesOf(json));

        assertAll(
                () -> assertEquals(TOPIC_NAME, parsed.get(MessageFields.TOPIC)),
                () -> assertNotNull(parsed.get(MessageFields.MQTT_RECEIVED_AT)),
                () -> assertInstanceOf(Long.class, parsed.get(MessageFields.MQTT_RECEIVED_AT)),
                () -> assertTrue(parsed.containsKey("deviceInfo")),
                () -> assertTrue(parsed.containsKey("object")),
                () -> assertFalse(parsed.containsKey("rawPayload"))
        );
    }

    @Test
    @DisplayName("JSON 형식이 깨진 payload는 예외 없이 rawPayload로 보관된다")
    void parseBrokenPayload() {
        MqttSubscriberNode node = createNode();

        Map<String, Object> parsed = node.parsePayload(TOPIC_NAME, bytesOf("not a json"));

        assertAll(
                () -> assertEquals("not a json", parsed.get("rawPayload")),
                () -> assertEquals(TOPIC_NAME, parsed.get(MessageFields.TOPIC)),
                () -> assertNotNull(parsed.get(MessageFields.MQTT_RECEIVED_AT))
        );
    }

    @Test
    @DisplayName("빈 payload도 예외 없이 rawPayload로 처리된다")
    void parseEmptyPayload() {
        MqttSubscriberNode node = createNode();

        Map<String, Object> parsed = node.parsePayload(TOPIC_NAME, new byte[0]);

        assertAll(
                () -> assertEquals("", parsed.get("rawPayload")),
                () -> assertEquals(TOPIC_NAME, parsed.get(MessageFields.TOPIC))
        );
    }

    @Test
    @DisplayName("payload에 topic 필드가 있어도 실제 수신 topic으로 덮어쓴다")
    void topicIsOverwritten() {
        MqttSubscriberNode node = createNode();
        String json = "{\"" + MessageFields.TOPIC + "\":\"fake/topic\"}";

        Map<String, Object> parsed = node.parsePayload(TOPIC_NAME, bytesOf(json));

        assertEquals(TOPIC_NAME, parsed.get(MessageFields.TOPIC));
    }

    @Test
    @DisplayName("DTO에 없는 필드가 있어도 파싱에 실패하지 않는다")
    void unknownFieldIsIgnored() {
        MqttSubscriberNode node = createNode();
        String json = "{\"unknownField\":\"value\",\"deviceInfo\":{\"devEui\":\"abc\"}}";

        Map<String, Object> parsed = node.parsePayload(TOPIC_NAME, bytesOf(json));

        assertAll(
                () -> assertFalse(parsed.containsKey("rawPayload")),
                () -> assertEquals("value", parsed.get("unknownField"))
        );
    }

    @Test
    @DisplayName("브로커에 접속할 수 없으면 ConnectionException으로 감싸서 던진다")
    void connectFailureWrapsException() {
        MqttSubscriberNode node = new MqttSubscriberNode("test", Map.of(
                MessageFields.BROKER_URL, UNREACHABLE_BROKER,
                MessageFields.CLIENT_ID, "test-client",
                MessageFields.TOPIC, TOPIC_NAME
        ));

        ConnectionException exception = assertThrows(ConnectionException.class, node::connect);

        assertInstanceOf(MqttException.class, exception.getCause());
    }

    @Test
    @DisplayName("브로커 URL에 스킴이 없으면 Paho가 IllegalArgumentException을 던진다")
    void malformedBrokerUrlIsNotWrapped() {
        MqttSubscriberNode node = new MqttSubscriberNode("test", Map.of(
                MessageFields.BROKER_URL, "invalid-url",
                MessageFields.CLIENT_ID, "test-client",
                MessageFields.TOPIC, TOPIC_NAME
        ));

        assertThrows(IllegalArgumentException.class, node::connect);
    }

    @Test
    @DisplayName("연결 실패시 initialize()는 예외를 던지지 않고 연결 상태만 실패로 남긴다")
    void initializeSurvivesConnectFailure() {
        MqttSubscriberNode node = new MqttSubscriberNode("test", Map.of(
                MessageFields.BROKER_URL, UNREACHABLE_BROKER,
                MessageFields.CLIENT_ID, "test-client",
                MessageFields.TOPIC, TOPIC_NAME,
                "maxCounts", 0
        ));

        assertDoesNotThrow(node::initialize);
        assertFalse(node.isConnected());

        node.shutdown();
    }

    @Test
    @DisplayName("연결한 적 없어도 shutdown()은 예외 없이 끝난다")
    void shutdownWithoutConnect() {
        MqttSubscriberNode node = createNode();

        assertDoesNotThrow(node::shutdown);
    }

    @Test
    @DisplayName("Subscriber 노드는 입력 메시지를 다음 노드로 전달하지 않는다")
    void onProcessDoesNotForward() {
        MqttSubscriberNode node = createNode();
        Connection connection = mock(Connection.class);
        node.getOutputPort("out").connect(connection);

        node.process(new Message(Map.of()));

        verify(connection, never()).deliver(any(Message.class));
    }
}
