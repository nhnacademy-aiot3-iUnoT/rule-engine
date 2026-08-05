package com.nhnacademy.ruleengine.global.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RabbitMqConfig {

    public static final String SENSOR_EXCHANGE = "iunot.sensor_exchange";

    public static final String SENSOR_RAW_QUEUE = "iunot.sensor.raw.queue";

    public static final String SENSOR_RAW_ROUTING_KEY = "iunot.sensor.raw";

    private static final String SENSOR_NORMALIZED_QUEUE_PREFIX = "iunot.sensor.normalized.queue.";
    private static final String SENSOR_NORMALIZED_ROUTING_KEY_PREFIX = "iunot.sensor.normalized.";
    public static final int SENSOR_NORMALIZED_PARTITION_COUNT = 4;

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
    // 정규화된 센서 데이터를 여러 Queue로 분산 처리하기 위한 파티션 Queue와 Binding을 생성한다.
    public Declarables sensorNormalizedPartitionedQueues(DirectExchange sensorExchange) {
        List<Declarable> declarables = new ArrayList<>();

        for (int partition = 0; partition < SENSOR_NORMALIZED_PARTITION_COUNT; partition++) {
            Queue queue = QueueBuilder.durable(normalizedQueueName(partition))
                    .singleActiveConsumer()
                    .build();

            Binding binding = BindingBuilder
                    .bind(queue)
                    .to(sensorExchange)
                    .with(normalizedRoutingKey(partition));

            declarables.add(queue);
            declarables.add(binding);
        }

        return new Declarables(declarables);
    }

    // 센서 식별자를 해시해 파티션 번호를 계산한다. 발행/구독 양쪽에서 반드시 동일한 키로 호출해야 한다.
    public static int normalizedPartitionOf(String sensorKey) {
        return Math.floorMod(sensorKey.hashCode(), SENSOR_NORMALIZED_PARTITION_COUNT);
    }

    public static String normalizedQueueName(int partition) {
        return SENSOR_NORMALIZED_QUEUE_PREFIX + partition;
    }

    public static String normalizedRoutingKey(int partition) {
        return SENSOR_NORMALIZED_ROUTING_KEY_PREFIX + partition;
    }

    public static String[] allNormalizedQueueNames() {
        String[] names = new String[SENSOR_NORMALIZED_PARTITION_COUNT];
        for (int partition = 0; partition < SENSOR_NORMALIZED_PARTITION_COUNT; partition++) {
            names[partition] = normalizedQueueName(partition);
        }
        return names;
    }
}
