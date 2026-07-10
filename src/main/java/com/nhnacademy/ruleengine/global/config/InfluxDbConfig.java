package com.nhnacademy.ruleengine.global.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(InfluxDbProperties.class)
// 설정값을 이용해 공유 InfluxDB 클라이언트를 생성한다.
public class InfluxDbConfig {

    @Bean
    public InfluxDBClient influxDBClient(InfluxDbProperties properties) {
        // 토큰 원문은 노출하지 않고 길이만 로그로 확인한다.
        log.info("Influx URL = {}", properties.url());
        log.info("Influx ORG = {}", properties.org());
        log.info("Influx BUCKET = {}", properties.bucket());
        log.info("Influx TOKEN length = {}",
                properties.token() == null ? null : properties.token().length()
        );

        return InfluxDBClientFactory.create(
                properties.url(),
                properties.token().toCharArray(),
                properties.org(),
                properties.bucket()
        );
    }
}
