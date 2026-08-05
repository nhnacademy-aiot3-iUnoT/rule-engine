package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RawSensorPublisher {
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final RabbitMqConfig rabbitMqConfig;

    public void publish(ExternalSensorMessage externalSensorMessage) {
        try {

            String payload = objectMapper.writeValueAsString(externalSensorMessage);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.SENSOR_EXCHANGE,
                    rabbitMqConfig.sensorRawRoutingKey(),
                    payload
            );

        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "외부데이터 직렬화 실패",
                    e
            );
        }

    }
}
