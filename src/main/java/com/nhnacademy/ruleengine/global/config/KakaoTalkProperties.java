package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.kakao-talk")
public record KakaoTalkProperties (
        String clientId,
        String apiBaseUrl,
        String accessToken
){
}
