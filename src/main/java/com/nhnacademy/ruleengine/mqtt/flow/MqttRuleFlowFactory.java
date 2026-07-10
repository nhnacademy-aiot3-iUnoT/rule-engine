package com.nhnacademy.ruleengine.mqtt.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;
import com.nhnacademy.ruleengine.mqtt.node.MqttPublisherNode;
import com.nhnacademy.ruleengine.mqtt.node.MqttSubscriberNode;
import com.nhnacademy.ruleengine.sensor.command.SensorCommand;
import com.nhnacademy.ruleengine.sensor.node.SensorFilterTransformNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MqttRuleFlowFactory {

    public static final String FLOW_ID = "external-mqtt-flow";

    static final String SUBSCRIBER_NODE_ID = "external-mqtt-in";
    static final String TRANSFORM_NODE_ID = "sensor-filter";
    static final String PUBLISHER_NODE_ID = "internal-mqtt-out";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final List<SensorCommand> sensorCommands;

    public Flow create() {
        ExternalConfig external = properties.mqtt().external();
        InternalConfig internal = properties.mqtt().internal();

        return new Flow(FLOW_ID)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        createExternalSubscriberConfig(external)
                ))
                .addNode(new SensorFilterTransformNode(
                        TRANSFORM_NODE_ID,
                        sensorCommands
                ))
                .addNode(new MqttPublisherNode(
                        PUBLISHER_NODE_ID,
                        createInternalPublisherConfig(internal)
                ))
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

    private Map<String, Object> createExternalSubscriberConfig(
            ExternalConfig external
    ) {
        if (external == null) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt.external-config 설정이 필요합니다."
            );
        }

        return Map.of(
                "brokerUrl",
                requireText(
                        external.brokerUrl(),
                        "external-config.broker-url"
                ),
                "clientId",
                requireText(
                        external.clientIdPrefix() + UUID.randomUUID(),
                        "external-config.client-id-prefix"
                ),
                "topic",
                requireText(
                        external.topic(),
                        "external-config.topic"
                ),
                "qos",
                validateQos(
                        external.qos(),
                        "external-config.qos"
                )
        );
    }

    private Map<String, Object> createInternalPublisherConfig(
            InternalConfig internal
    ) {
        if (internal == null) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt.internal-config 설정이 필요합니다."
            );
        }

        return Map.of(
                "brokerUrl",
                requireText(
                        internal.brokerUrl(),
                        "internal-config.broker-url"
                ),
                "clientId",
                requireText(
                        internal.clientIdPrefix() + UUID.randomUUID(),
                        "internal-config.client-id-prefix"
                ),
                "qos",
                validateQos(
                        internal.qos(),
                        "internal-config.qos"
                ),
                "topicPrefix",
                normalizeTopicPrefix(internal.topicPrefix())
        );
    }

    private String normalizeTopicPrefix(String topicPrefix) {
        if (topicPrefix == null || topicPrefix.isBlank()) {
            return "";
        }

        return topicPrefix.trim()
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    private String requireText(
            String value,
            String propertyName
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt."
                            + propertyName
                            + " 설정이 필요합니다."
            );
        }

        return value.trim();
    }

    private int validateQos(
            int qos,
            String propertyName
    ) {
        if (qos < 0 || qos > 2) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt."
                            + propertyName
                            + "는 0~2 사이여야 합니다: "
                            + qos
            );
        }

        return qos;
    }
}