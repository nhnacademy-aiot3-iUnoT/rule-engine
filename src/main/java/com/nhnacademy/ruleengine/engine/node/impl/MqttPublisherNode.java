package com.nhnacademy.ruleengine.engine.node.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.node.ProtocolNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.io.IOException;
import java.util.Map;


@Slf4j
// 변환된 센서 payload를 내부 MQTT broker로 발행한다.
public class MqttPublisherNode extends ProtocolNode {
    private MqttClient client;
    private final ObjectMapper objectMapper;

    public MqttPublisherNode(String id, Map<String, Object> config) {
        super(id, config);
        this.objectMapper = new ObjectMapper();

        addInputPort("in");
    }

    @Override
    protected void connect() throws Exception {
        // 설정된 내부 broker로 발행용 MQTT 연결을 생성한다.
        String clientId = (String) getConfig("clientId");
        String brokerUrl = (String) getConfig("brokerUrl");

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        closeClient();
        // 프로젝트에 Paho 임시 폴더가 생기지 않도록 메모리 저장소를 사용한다.
        client = new MqttClient(
                brokerUrl,
                clientId,
                new MemoryPersistence()
        );
        client.connect(options);

        log.info("[{}] MQTT client connected status : {}", getId(), getConnectionState());
        log.info("MQTT client-id : {}", clientId);

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
        }
    }

    @Override
    protected void onProcess(Message message) {
        // topic과 payload를 추출해 JSON MQTT 메시지로 발행한다.
        try {
            String topic = resolveTopic(message);

            int qos = resolveQos(getConfig("qos"));

            Object outboundPayload = message.hasEntry(MessageFields.SENSOR_PAYLOAD)
                    ? message.get(MessageFields.SENSOR_PAYLOAD)
                    : message.getPayload();
            byte[] payload = objectMapper.writeValueAsBytes(outboundPayload);

            MqttMessage jsonMessage = new MqttMessage(payload);
            jsonMessage.setQos(qos);

            if (client != null && client.isConnected()) {
                client.publish(topic, jsonMessage);
                log.info("[{}] 메시지 발행 성공: {}", getId(), topic);
            } else {
                log.error("[{}] 브로커와 연결되지 않았습니다", getId());
            }

        } catch (IOException e) {
            log.error("[{}] Jackson 직렬화 실패: {}", getId(), e.getMessage());
        } catch (MqttException e) {
            log.error("[{}] MQTT 발행 실패: {}", getId(), e.getMessage());
        }
    }

    private int resolveQos(Object objectQos) {
        // 숫자 또는 문자열 QoS를 정수로 변환하고 범위를 보정한다.
        int qos = 1;
        if (objectQos instanceof Number number) {
            qos = number.intValue();
        } else if (objectQos instanceof String text && !text.isBlank()) {
            qos = Integer.parseInt(text);
        }

        if (qos < 0 || qos > 2) {
            log.warn("[{}] 잘못된 MQTT QoS 값입니다. qos={}, default=1", getId(), qos);
            return 1;
        }

        return qos;
    }

    private String resolveTopic(Message message) {
        String topic = message.get(MessageFields.TOPIC);
        String topicPrefix = (String) getConfig("topicPrefix");

        if (topic == null || topic.isBlank()) {
            topic = (String) getConfig("topic");
        }

        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException(
                    "MQTT publish topic is required."
            );
        }

        if (topicPrefix == null || topicPrefix.isBlank()) {
            return topic;
        }

        if (topic.startsWith(topicPrefix + "/")) {
            return topic;
        }

        return topicPrefix + "/" + topic;
    }

}
