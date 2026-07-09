package com.nhnacademy.ruleengine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "influxdb")
public record InfluxDbProperties(
        String url,
        String token,
        String org,
        String bucket,
        String measurement
) {
}
