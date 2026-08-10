package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;


@ConfigurationProperties(prefix = "rule-engine.inventory")
public record InventoryProperties(
        String baseUrl
) {
    public InventoryProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.inventory.base-url은 필수입니다."
            );
        }
    }
}
