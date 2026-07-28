package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NormalizedSensorPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public void publish(SensorPayload sensorPayload) {
        try {
            String payload = objectMapper.writeValueAsString(sensorPayload);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.SENSOR_EXCHANGE,
                    RabbitMqConfig.SENSOR_NORMALIZED_ROUTING_KEY,
                    payload
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "표준 센서 데이터 직렬화 실패",
                    e
            );
        }
    }

}
