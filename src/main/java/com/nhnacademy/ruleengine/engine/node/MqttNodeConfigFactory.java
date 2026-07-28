package com.nhnacademy.ruleengine.engine.node;

import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class MqttNodeConfigFactory {

    private static final String BROKER_URL = "brokerUrl";
    private static final String CLIENT_ID = "clientId";
    private static final String TOPIC = "topic";
    private static final String QOS = "qos";

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
