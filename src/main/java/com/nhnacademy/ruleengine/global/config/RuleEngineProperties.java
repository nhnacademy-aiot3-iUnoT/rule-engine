package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rule-engine")
public record RuleEngineProperties(
        Mqtt mqtt
) {

    public record Mqtt(
            ExternalConfig external,
            InternalConfig internal
    ) {
    }

    public record ExternalConfig(
            String brokerUrl,
            String clientIdPrefix,
            String topic,
            int qos
    ) {
    }

    public record InternalConfig(
            String brokerUrl,
            String clientIdPrefix,
            String topicPrefix,
            int qos
    ) {
    }
}
