package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rule-engine")
// application.yaml의 Rule Engine 설정을 타입 안전하게 바인딩한다.
public record RuleEngineProperties(
        Mqtt mqtt
) {
    public record Mqtt(
            MqttInbound inbound,
            MqttOutbound outbound
    ) {
    }

    public record MqttInbound(
            boolean enabled,
            String brokerUrl,
            String clientId,
            String topic,
            int qos
    ) {
    }

    public record MqttOutbound(
            boolean enabled,
            String brokerUrl,
            String clientId,
            String topicPrefix,
            int qos
    ) {
    }
}
