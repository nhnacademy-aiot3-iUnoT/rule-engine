package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.SensorTransformService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

// 외부 MQTT 메시지를 표준 센서 payload로 변환한다.
// 외부 메시지 하나에 온도·습도·조도 등 여러 측정값이 들어있으므로 payload 개수만큼 나눠서 내보낸다.
@Slf4j
public class SensorTransformNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final SensorTransformService sensorTransformService;

    public SensorTransformNode(String id, SensorTransformService sensorTransformService) {
        super(id);

        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        this.sensorTransformService = sensorTransformService;
    }

    @Override
    protected void onProcess(Message message) {
        ExternalSensorMessage externalSensorMessage = message.get(MessageFields.EXTERNAL_SENSOR_MESSAGE);

        if (externalSensorMessage == null) {
            log.warn("[{}] 외부 센서 메시지가 없습니다.", getId());
            return;
        }

        List<SensorPayload> sensorPayloads = sensorTransformService.transform(externalSensorMessage);

        for (SensorPayload sensorPayload : sensorPayloads) {
            send(
                    OUTPUT_PORT,
                    new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload))
            );
        }

        log.debug("[{}] 외부 센서 메시지 변환 완료. normalizedCount={}", getId(), sensorPayloads.size());
    }
}
