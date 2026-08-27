package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorKeys;
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
    private final RabbitMqConfig rabbitMqConfig;

    public void publish(SensorPayload sensorPayload) {
        try {
            String payload = objectMapper.writeValueAsString(sensorPayload);
            String routingKey = normalizedRoutingKeyFor(sensorPayload);

            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.SENSOR_EXCHANGE,
                    routingKey,
                    payload
            );
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "표준 센서 데이터 직렬화 실패",
                    e
            );
        }
    }

    // 같은 센서는 항상 같은 파티션 큐로 가도록 라우팅 키를 계산한다.
    private String normalizedRoutingKeyFor(SensorPayload sensorPayload) {
        String sensorKey = SensorKeys.of(
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sensorPayload.zoneId(),
                sensorPayload.deviceEui(),
                sensorPayload.sensorType()
        );

        int partition = RabbitMqConfig.normalizedPartitionOf(sensorKey);
        return rabbitMqConfig.normalizedRoutingKey(partition);
    }
}
