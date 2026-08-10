package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.telegram")
public record TelegramProperties(
        String botToken,
        String apiBaseUrl
) {
}
