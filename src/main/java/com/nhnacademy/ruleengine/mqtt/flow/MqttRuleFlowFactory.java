package com.nhnacademy.ruleengine.mqtt.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.mqtt.node.MqttPublisherNode;
import com.nhnacademy.ruleengine.mqtt.node.MqttSubscriberNode;
import com.nhnacademy.ruleengine.sensor.command.SensorCommand;
import com.nhnacademy.ruleengine.sensor.node.SensorFilterTransformNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
// 외부 MQTT 메시지를 변환해 내부 MQTT로 보내는 Flow를 구성한다.
public class MqttRuleFlowFactory {

    public static final String FLOW_ID = "external-mqtt-flow";
    static final String SUBSCRIBER_NODE_ID = "external-mqtt-in";
    static final String TRANSFORM_NODE_ID = "sensor-filter";
    static final String PUBLISHER_NODE_ID = "internal-mqtt-out";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    // 등록된 센서 변환 전략을 TransformNode에 전달한다.
    private final List<SensorCommand> sensorCommands;

    public Flow create() {
        // Flow 생성 전에 필수 MQTT 설정을 확인한다.
        RuleEngineProperties.Mqtt mqtt = Objects.requireNonNull(
                properties.mqtt(),
                "rule-engine.mqtt 설정이 필요합니다."
        );
        RuleEngineProperties.MqttInbound inbound = Objects.requireNonNull(
                mqtt.inbound(),
                "rule-engine.mqtt.inbound 설정이 필요합니다."
        );
        RuleEngineProperties.MqttOutbound outbound = Objects.requireNonNull(
                mqtt.outbound(),
                "rule-engine.mqtt.outbound 설정이 필요합니다."
        );

        // 구독 → 센서 변환 → 발행 순서로 노드와 포트를 연결한다.
        return new Flow(FLOW_ID)
                .addNode(
                        new MqttSubscriberNode(
                                SUBSCRIBER_NODE_ID,
                                inboundConfig(inbound)
                        )
                )
                .addNode(
                        new SensorFilterTransformNode(
                                TRANSFORM_NODE_ID,
                                sensorCommands
                        )
                )
                .addNode(
                        new MqttPublisherNode(
                                PUBLISHER_NODE_ID,
                                outboundConfig(outbound)
                        )
                )
                .connect(
                        SUBSCRIBER_NODE_ID,
                        OUTPUT_PORT,
                        TRANSFORM_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        TRANSFORM_NODE_ID,
                        OUTPUT_PORT,
                        PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

    private Map<String, Object> inboundConfig(
            RuleEngineProperties.MqttInbound inbound
    ) {
        return Map.of(
                "brokerUrl", requireText(inbound.brokerUrl(), "inbound.broker-url"),
                "clientId", requireText(inbound.clientId(), "inbound.client-id"),
                "topic", requireText(inbound.topic(), "inbound.topic"),
                "qos", validateQos(inbound.qos(), "inbound.qos")
        );
    }

    private Map<String, Object> outboundConfig(
            RuleEngineProperties.MqttOutbound outbound
    ) {
        return Map.of(
                "brokerUrl", requireText(outbound.brokerUrl(), "outbound.broker-url"),
                "clientId", requireText(outbound.clientId(), "outbound.client-id"),
                "qos", validateQos(outbound.qos(), "outbound.qos"),
                "topicPrefix", outbound.topicPrefix() == null ? "" : outbound.topicPrefix().trim()
        );
    }

    private String requireText(String value, String propertyName) {
        // 연결 전에 누락된 필수 설정을 차단한다.
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt." + propertyName + " 설정이 필요합니다."
            );
        }
        return value.trim();
    }

    private int validateQos(int qos, String propertyName) {
        // MQTT QoS는 0, 1, 2만 허용한다.
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt." + propertyName + "는 0~2 사이여야 합니다: " + qos
            );
        }
        return qos;
    }
}
