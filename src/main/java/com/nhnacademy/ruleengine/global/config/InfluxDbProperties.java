package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "influx-db")
// application 설정의 InfluxDB 접속 및 저장 정보를 담는다.
public record InfluxDbProperties(
        String url,
        String token,
        String org,
        String bucket,
        String measurement
) {
}
