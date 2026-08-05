package com.nhnacademy.ruleengine.engine.node.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.FlowProcessingCompletion;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.global.config.RabbitMqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
public class NormalizedSensorConsumerNode extends AbstractNode {

    public static final String NODE_ID = "normalized-rabbit-consumer";

    private static final String OUTPUT_PORT = "out";
    private static final Duration PROCESSING_TIMEOUT = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;

    public NormalizedSensorConsumerNode(ObjectMapper objectMapper) {
        super(NODE_ID);
        this.objectMapper = objectMapper;
        addOutputPort(OUTPUT_PORT);
    }

    @RabbitListener(
            queues = "#{T(com.nhnacademy.ruleengine.global.config.RabbitMqConfig).allNormalizedQueueNames()}"
    )
    public void consume(String rawPayload) {
        SensorPayload sensorPayload = deserialize(rawPayload);
        if (sensorPayload == null) {
            return;
        }

        FlowProcessingCompletion completion = new FlowProcessingCompletion();

        Message message = new Message(
                Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload),
                completion
        );

        process(message);
        awaitFlowCompletion(completion);
    }

    @Override
    protected void onProcess(Message message) {
        send(OUTPUT_PORT, message);
    }

    private SensorPayload deserialize(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, SensorPayload.class);
        } catch (JsonProcessingException exception) {
            log.error(
                    "표준 센서 메시지 역직렬화 실패. payload={}",
                    rawPayload,
                    exception
            );
            return null;
        }
    }

    private void awaitFlowCompletion(FlowProcessingCompletion completion) {
        try {
            completion.await(PROCESSING_TIMEOUT);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AmqpException("표준 센서 Flow 대기 중 중단되었습니다.", exception);
        } catch (TimeoutException exception) {
            throw new AmqpException("표준 센서 Flow 처리 시간이 초과되었습니다.", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof IllegalArgumentException) {
                throw new AmqpRejectAndDontRequeueException(
                        "표준 센서 데이터 검증에 실패했습니다.",
                        cause
                );
            }

            throw new AmqpException("표준 센서 Flow 처리에 실패했습니다.", cause);
        }
    }
}
