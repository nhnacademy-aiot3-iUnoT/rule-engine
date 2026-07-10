package com.nhnacademy.ruleengine.mqtt.node;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.node.ProtocolNode;
import com.nhnacademy.ruleengine.mqtt.dto.MqttInboundMessageDto;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
// 외부 MQTT topic을 구독해 Rule Engine 메시지로 변환한다.
public class MqttSubscriberNode extends ProtocolNode {
    private MqttClient client;
    private final ObjectMapper objectMapper;
    private String targetTopic;

    public MqttSubscriberNode(String id, Map<String, Object> config) {
        super(id, config);
        this.objectMapper = new ObjectMapper();
        addOutputPort("out");
    }

    @Override
    protected void connect() throws Exception {
        // 설정된 broker와 topic으로 MQTT 구독 연결을 생성한다.
        String brokerUrl = (String) getConfig("brokerUrl");
        String clientId = (String) getConfig("clientId");
        this.targetTopic = (String) getConfig("topic");

        Object objectQos = getConfig("qos");
        int qos = resolveQos(objectQos);

        closeClient();
        client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

        client.setCallback(new MqttCallbackExtended() {
            @Override
            public void messageArrived(String receivedTopic, MqttMessage msg) {
                // 구독 조건에 맞는 메시지만 DTO로 변환해 출력한다.
                if (receivedTopic == null || !MqttTopic.isMatched(targetTopic, receivedTopic)) {
                    return;
                }

                Map<String, Object> payloadMap = processPayload(receivedTopic, msg.getPayload());
                MqttInboundMessageDto mqttInbound = MqttInboundMessageDto.from(payloadMap);
                send("out", new Message(Map.of("mqttInbound", mqttInbound)));

                log.info("[{}] 메시지 수집 완료", getId());
            }

            @Override
            public void connectionLost(Throwable cause) {
                log.warn("[{}] MQTT 연결 끊김: {}", getId(), cause.getMessage());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 사용하지 않음
            }

            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                // 자동 재연결 후 끊어진 topic 구독을 복구한다.
                if (reconnect) {
                    try {
                        client.subscribe(targetTopic, qos);
                        log.info("[{}] MQTT 재연결 후 구독 복구: {}", getId(), targetTopic);
                    } catch (MqttException e) {
                        log.error("[{}] MQTT 재구독 실패: {}", getId(), e.getMessage());
                    }
                }
            }
        });

        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);

        client.connect(options);
        client.subscribe(targetTopic, qos);

        log.info("[{}] 구독 성공: {}", getId(), targetTopic);
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

    protected Map<String, Object> processPayload(String topic, byte[] payloadBytes) {
        // JSON 파싱 실패 시 원문을 보존해 메시지 유실을 막는다.
        Map<String, Object> payloadMap;
        try {
            payloadMap = objectMapper.readValue(payloadBytes, new TypeReference<>() {
            });
        } catch (Exception e) {
            payloadMap = new HashMap<>();
            payloadMap.put("rawPayload", new String(payloadBytes, StandardCharsets.UTF_8));
            log.warn("[{}] 파싱 실패, rawPayload 생성", getId());
        }

        payloadMap.put("topic", topic);
        payloadMap.put("mqttTimestamp", System.currentTimeMillis());

        return payloadMap;
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
        // Subscriber 노드는 외부 이벤트를 수신하므로 본 메서드는 사용하지 않음
    }
}
