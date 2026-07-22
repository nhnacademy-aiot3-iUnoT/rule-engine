package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.MessageFields;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.validation.SensorPayloadValidator;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// 표준 센서 DTO의 필수값을 검증하고 정상 메시지만 전달한다.
public class SensorPayloadValidationNode extends AbstractNode {
    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    public SensorPayloadValidationNode(String id) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayloadDto sensorPayload = message.get(MessageFields.SENSOR_PAYLOAD);
        if (sensorPayload == null) {
            log.warn("[{}] sensorPayload가 없어 검증을 건너뜁니다.", getId());
            return;
        }

        try {
            SensorPayloadValidator.validate(sensorPayload);
            send(OUTPUT_PORT, message);
        } catch (IllegalArgumentException e) {
            log.warn(
                    "[{}] 내부 센서 payload 검증 실패. reason={}",
                    getId(),
                    e.getMessage()
            );
        }
    }
}
