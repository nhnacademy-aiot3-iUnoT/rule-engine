package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.service.SensorTransformService;
import com.nhnacademy.ruleengine.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawSensorConsumer {

    private final ObjectMapper objectMapper;
    private final SensorTransformService sensorTransformService;
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    @RabbitListener(
            queues = RabbitMqConfig.SENSOR_RAW_QUEUE
    )
    public void consume(String rawPayload) {
        ExternalSensorMessage externalSensorMessage;

        log.info("Received sensor raw message: {}", rawPayload);

        try {
            externalSensorMessage = objectMapper.readValue(rawPayload, ExternalSensorMessage.class);
        } catch (JsonProcessingException e) {
            log.error(
                    "외부 센서 메시지 역직렬화 실패. payload={}",
                    rawPayload,
                    e
            );
            return;
        }

        List<SensorPayload> sensorPayloads = sensorTransformService.transform(externalSensorMessage);


        sensorPayloads.forEach(
                normalizedSensorPublisher::publish
        );

        log.info(
                "외부 센서 메시지 변환 완료. normalizedCount={}",
                sensorPayloads.size()
        );

    }
}
