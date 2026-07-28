package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rule-engine")
public record RuleEngineProperties(
        Mqtt mqtt
) {

    public record Mqtt(
            ExternalConfig external
    ) {
    }

    public record ExternalConfig(
            String brokerUrl,
            String clientIdPrefix,
            String topic,
            int qos
    ) {
    }

}
