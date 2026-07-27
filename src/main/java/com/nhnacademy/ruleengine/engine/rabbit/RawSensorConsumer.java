package com.nhnacademy.ruleengine.engine.rabbit;

import com.nhnacademy.ruleengine.global.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RawSensorConsumer {

    @RabbitListener(
            queues = RabbitMqConfig.SENSOR_RAW_QUEUE
    )
    public void consume(String payload) {
        log.info("Received sensor raw message: {}", payload);
    }
}
