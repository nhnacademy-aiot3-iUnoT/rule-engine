package com.nhnacademy.ruleengine.engine.node.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.ProtocolNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static com.nhnacademy.ruleengine.engine.constants.MessageFields.*;

@Slf4j
// MQTT topic을 구독하고, 수신한 payload를 Rule Engine 메시지로 변환한다.
public class MqttSubscriberNode extends ProtocolNode {
    private static final String OUTPUT_PORT = "out";



    private static final int DEFAULT_QOS = 1;

    private final ObjectMapper objectMapper;

    private MqttClient client;
    private String subscriptionTopic;
    private int subscriptionQos;

    public MqttSubscriberNode(String id, Map<String, Object> config) {
        super(id, config);
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false); // DTO에 없는 필드 무시
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void connect() throws Exception {
        String brokerUrl = (String) getConfig(BROKER_URL);
        String clientId = (String) getConfig(CLIENT_ID);
        subscriptionTopic = (String) getConfig(TOPIC);
        subscriptionQos = resolveQos(getConfig(QOS));

        closeClient();
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
        client.setCallback(createCallback());
        client.connect(createConnectOptions());
        subscribe();

        log.info("[{}] MQTT 구독 성공: topic={}, qos={}", getId(), subscriptionTopic, subscriptionQos);
    }

    private MqttCallbackExtended createCallback() {
        return new MqttCallbackExtended() {
            @Override
            public void messageArrived(String receivedTopic, MqttMessage message) {
                handleIncomingMessage(receivedTopic, message);
            }

            @Override
            public void connectionLost(Throwable cause) {
                log.warn("[{}] MQTT 연결 끊김", getId(), cause);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 사용하지 않음
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect) {
                    restoreSubscription();
                }
            }
        };
    }

    private MqttConnectOptions createConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        return options;
    }

    private void handleIncomingMessage(String receivedTopic, MqttMessage message) {
        if (!isSubscribedTopic(receivedTopic)) {
            return;
        }

        Map<String, Object> receivedPayload = parsePayload(receivedTopic, message.getPayload());
        sendReceivedPayload(receivedPayload);
        log.info("[{}] MQTT 메시지 수신 완료: topic={}", getId(), receivedTopic);
    }

    private boolean isSubscribedTopic(String receivedTopic) {
        return receivedTopic != null && MqttTopic.isMatched(subscriptionTopic, receivedTopic);
    }

    private void sendReceivedPayload(Map<String, Object> receivedPayload) {

        // 내부 MQTT 구독시
        if (receivesStandardSensorPayload()) {
            sendStandardSensorPayload(receivedPayload);
            return;
        }

        // 외부 MQTT 구독시
        ExternalSensorMessage externalSensorMessage = ExternalSensorMessage.from(receivedPayload);
        send(OUTPUT_PORT, new Message(Map.of(
                MessageFields.EXTERNAL_SENSOR_MESSAGE,
                externalSensorMessage
        )));
    }

    // 내부 MQTT 타입의 데이터인지 확인
    private boolean receivesStandardSensorPayload() {
        return SENSOR_PAYLOAD.equals(getConfig(PAYLOAD_TYPE));
    }

    // 내부 MQTT 수신 로직
    private void sendStandardSensorPayload(Map<String, Object> receivedPayload) {
        try {
            SensorPayload sensorPayload = objectMapper.convertValue(
                    receivedPayload,
                    SensorPayload.class
            );
            send(OUTPUT_PORT, new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload)));
        } catch (IllegalArgumentException e) {
            log.warn(
                    "[{}] 내부 센서 DTO 변환 실패. topic={}, reason={}",
                    getId(),
                    receivedPayload.get(MessageFields.TOPIC),
                    e.getMessage()
            );
        }
    }

    private int resolveQos(Object configuredQos) {
        int qos = DEFAULT_QOS;
        if (configuredQos instanceof Number number) {
            qos = number.intValue();
        } else if (configuredQos instanceof String text && !text.isBlank()) {
            qos = Integer.parseInt(text);
        }

        if (qos < 0 || qos > 2) {
            log.warn("[{}] 잘못된 MQTT QoS 값입니다. qos={}, default={}", getId(), qos, DEFAULT_QOS);
            return DEFAULT_QOS;
        }

        return qos;
    }

    protected Map<String, Object> parsePayload(String topic, byte[] payloadBytes) {
        Map<String, Object> payloadMap;
        try {
            payloadMap = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (Exception e) {
            payloadMap = new HashMap<>();
            payloadMap.put("rawPayload", new String(payloadBytes, StandardCharsets.UTF_8));
            log.warn("[{}] 파싱 실패, rawPayload 생성", getId());
        }

        payloadMap.put(MessageFields.TOPIC, topic);
        payloadMap.put(MessageFields.MQTT_RECEIVED_AT, System.currentTimeMillis());

        return payloadMap;
    }

    private void subscribe() throws MqttException {
        client.subscribe(subscriptionTopic, subscriptionQos);
    }

    private void restoreSubscription() {
        try {
            subscribe();
            log.info("[{}] MQTT 재연결 후 구독 복구: {}", getId(), subscriptionTopic);
        } catch (MqttException e) {
            log.error("[{}] MQTT 재구독 실패: {}", getId(), e.getMessage());
        }
    }

    @Override
    protected void disconnect() {
        closeClient();
    }

    private void closeClient() {
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect();
            }
            client.close();
        } catch (MqttException e) {
            log.error("[{}] MQTT 종료 중 오류: {}", getId(), e.getMessage());
        } finally {
            client = null;
        }
    }

    @Override
    protected void onProcess(Message message) {
        // Subscriber 노드는 외부 이벤트를 수신하므로 본 메서드는 사용하지 않음
    }
}
