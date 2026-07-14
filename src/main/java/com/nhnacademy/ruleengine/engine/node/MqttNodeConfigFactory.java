package com.nhnacademy.ruleengine.engine.node;

import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class MqttNodeConfigFactory {

    private static final String BROKER_URL = "brokerUrl";
    private static final String CLIENT_ID = "clientId";
    private static final String TOPIC = "topic";
    private static final String TOPIC_PREFIX = "topicPrefix";
    private static final String QOS = "qos";
    private static final String PAYLOAD_TYPE = "payloadType";
    private static final String EXTERNAL_SENSOR_PAYLOAD = "externalSensor";
    private static final String STANDARD_SENSOR_PAYLOAD = "sensorPayload";

    public Map<String, Object> createExternalSubscriberConfig(
            ExternalConfig external
    ) {
        if (external == null) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt.external 설정이 필요합니다."
            );
        }

        return Map.of(
                BROKER_URL,
                requireText(
                        external.brokerUrl(),
                        "external.broker-url"
                ),
                CLIENT_ID,
                createClientId(
                        external.clientIdPrefix(),
                        "external.client-id-prefix"
                ),
                TOPIC,
                requireText(
                        external.topic(),
                        "external.topic"
                ),
                QOS,
                validateQos(
                        external.qos(),
                        "external.qos"
                ),
                PAYLOAD_TYPE,
                EXTERNAL_SENSOR_PAYLOAD
        );
    }

    public Map<String, Object> createInternalSubscriberConfig(
            InternalConfig internal,
            String topicSuffix
    ) {
        if (internal == null) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt.internal 설정이 필요합니다."
            );
        }

        return Map.of(
                BROKER_URL,
                requireText(
                        internal.brokerUrl(),
                        "internal.broker-url"
                ),
                CLIENT_ID,
                createClientId(
                        internal.clientIdPrefix(),
                        "internal.client-id-prefix"
                ),
                TOPIC,
                requireText(
                        normalizeTopic(internal.topicPrefix(), topicSuffix),
                        "internal.topic-prefix"
                ),
                QOS,
                validateQos(
                        internal.qos(),
                        "internal.qos"
                ),
                PAYLOAD_TYPE,
                STANDARD_SENSOR_PAYLOAD
        );
    }

    public Map<String, Object> createInternalPublisherConfig(
            InternalConfig internal
    ) {
        if (internal == null) {
            throw new IllegalArgumentException(
                    "rule-engine.mqtt.internal 설정이 필요합니다."
            );
        }

        return Map.of(
                BROKER_URL,
                requireText(
                        internal.brokerUrl(),
                        "internal.broker-url"
                ),
                CLIENT_ID,
                createClientId(
                        internal.clientIdPrefix(),
                        "internal.client-id-prefix"
                ),
                TOPIC_PREFIX,
                requireText(
                        internal.topicPrefix(),
                        "internal.topic-prefix"
                ),
                QOS,
                validateQos(
                        internal.qos(),
                        "internal.qos"
                )
        );
    }

    private String createClientId(
            String prefix,
            String propertyName
    ) {
        return requireText(prefix, propertyName)
                + "-"
                + UUID.randomUUID();
    }

    private String normalizeTopic(
            String topicPrefix,
            String topicSuffix
    ) {
        String prefix = normalizeTopicPart(topicPrefix);
        String suffix = normalizeTopicPart(topicSuffix);

        if (prefix.isEmpty()) {
            return suffix;
        }

        if (suffix.isEmpty()) {
            return prefix;
        }

        return prefix + "/" + suffix;
    }

    private String normalizeTopicPart(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }

        return value.trim()
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
