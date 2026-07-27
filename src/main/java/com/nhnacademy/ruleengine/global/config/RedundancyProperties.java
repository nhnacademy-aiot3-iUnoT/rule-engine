package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "rule-engine.redundancy"
)
public record RedundancyProperties(
        String instanceId,
        ExternalIngress externalIngress
) {
    public record ExternalIngress(
            String lockKey,
            Duration leaseDuration,
            Duration renewInterval
    ) {

    }
}
