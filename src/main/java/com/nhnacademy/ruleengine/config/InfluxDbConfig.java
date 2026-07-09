package com.nhnacademy.ruleengine.config;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(InfluxDbProperties.class)
public class InfluxDbConfig {

    @Bean
    public InfluxDBClient influxDBClient(InfluxDbProperties properties) {
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