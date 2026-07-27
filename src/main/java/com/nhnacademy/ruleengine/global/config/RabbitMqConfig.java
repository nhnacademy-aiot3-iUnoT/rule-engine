package com.nhnacademy.ruleengine.global.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static final String SENSOR_EXCHANGE = "iunot.sensor_exchange";

    public static final String SENSOR_RAW_QUEUE = "iunot.sensor.raw.queue";

    public static final String SENSOR_RAW_ROUTING_KEY = "iunot.sensor.raw";

    public static final String SENSOR_NORMALIZED_QUEUE = "iunot.sensor.normalized.queue";

    public static final String SENSOR_NORMALIZED_ROUTING_KEY = "iunot.sensor.normalized";

    @Bean
    public DirectExchange sensorExchange() {
        return new DirectExchange(
                SENSOR_EXCHANGE,
                true, // 재시작 이후에도 유지
                false // 사용자가 없어도 삭제되지 않음
        );
    }

    @Bean
    public Queue sensorRawQueue() {
        return QueueBuilder.durable(SENSOR_RAW_QUEUE)
                .build();
    }

    @Bean
    public Binding sensorRawBinding(DirectExchange sensorExchange, @Qualifier("sensorRawQueue") Queue sensorRawQueue) {
        return BindingBuilder
                .bind(sensorRawQueue)
                .to(sensorExchange)
                .with(SENSOR_RAW_ROUTING_KEY);
    }

    @Bean
    public Queue sensorNormalizedQueue() {
        return QueueBuilder.durable(SENSOR_NORMALIZED_QUEUE)
                .build();
    }

    @Bean
    public Binding sensorNormalizedBinding(DirectExchange sensorExchange, @Qualifier("sensorNormalizedQueue") Queue sensorNormalizedQueue) {
        return BindingBuilder
                .bind(sensorNormalizedQueue)
                .to(sensorExchange)
                .with(SENSOR_NORMALIZED_ROUTING_KEY);
    }
}
