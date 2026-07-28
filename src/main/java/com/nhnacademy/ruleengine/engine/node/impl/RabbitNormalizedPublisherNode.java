package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RabbitNormalizedPublisherNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    public RabbitNormalizedPublisherNode(String id, NormalizedSensorPublisher normalizedSensorPublisher) {
        super(id);

        addInputPort(INPUT_PORT);
        this.normalizedSensorPublisher = normalizedSensorPublisher;
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayload sensorPayload = message.get(MessageFields.SENSOR_PAYLOAD);
        if (sensorPayload == null) {
            log.warn("[{}] 내부 센서 메세지 존재하지않습니다", getId());
            return;
        }

        normalizedSensorPublisher.publish(sensorPayload);
    }
}
