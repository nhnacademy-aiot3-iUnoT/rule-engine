package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.rabbit.RawSensorPublisher;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RabbitRawPublisherNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private final RawSensorPublisher rawSensorPublisher;

    public RabbitRawPublisherNode(String id, RawSensorPublisher rawSensorPublisher) {
        super(id);

        addInputPort(INPUT_PORT);
        this.rawSensorPublisher = rawSensorPublisher;
    }

    @Override
    protected void onProcess(Message message) {
        ExternalSensorMessage externalSensorMessage = message.get(MessageFields.EXTERNAL_SENSOR_MESSAGE);
        if (externalSensorMessage != null) {
            log.warn("[{}] 외부 센서 메세지 없습니다", getId());
        }
        rawSensorPublisher.publish(externalSensorMessage);
    }
}
