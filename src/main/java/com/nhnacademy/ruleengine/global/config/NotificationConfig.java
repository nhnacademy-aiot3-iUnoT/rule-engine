package com.nhnacademy.ruleengine.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class NotificationConfig {

    @Bean
    public RestClient telegramRestClient(TelegramProperties properties){
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .build();
    }

    @Bean
    public RestClient kakaoRestClient(KakaoTalkProperties properties){
        return RestClient.builder()
                .baseUrl(properties.apiBaseUrl())
                .build();
    }
}
