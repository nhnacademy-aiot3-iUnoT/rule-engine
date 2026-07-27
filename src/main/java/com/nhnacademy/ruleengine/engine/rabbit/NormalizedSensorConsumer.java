package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.service.EnvironmentProcessingService;
import com.nhnacademy.ruleengine.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NormalizedSensorConsumer {

    private final ObjectMapper objectMapper;
    private final EnvironmentProcessingService processingService;

    @RabbitListener(
            queues = RabbitMqConfig.SENSOR_NORMALIZED_QUEUE
    )
    public void consume(String rawPayload) {
        SensorPayload sensorPayload;

        try {
            sensorPayload = objectMapper.readValue(
                    rawPayload,
                    SensorPayload.class
            );
        } catch (JsonProcessingException exception) {
            log.error(
                    "표준 센서 메시지 역직렬화 실패. payload={}",
                    rawPayload,
                    exception
            );
            return;
        }

        try {
            processingService.process(sensorPayload);
        } catch (IllegalArgumentException exception) {
            log.warn(
                    "표준 센서 데이터 검증 실패. reason={}, payload={}",
                    exception.getMessage(),
                    sensorPayload
            );
        }
    }
}