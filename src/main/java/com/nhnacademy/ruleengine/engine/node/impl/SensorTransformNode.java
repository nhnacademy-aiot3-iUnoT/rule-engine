package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.SensorTransformService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
// MQTT 측정값을 센서 타입별 표준 payload로 변환한다.
public class SensorTransformNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final SensorTransformService sensorTransformService;

    public SensorTransformNode(
            String id,
            SensorTransformService sensorTransformService
    ) {
        super(id);
        this.sensorTransformService = sensorTransformService;
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    public void onProcess(Message message) {
        ExternalSensorMessage externalSensorMessage = message.get(MessageFields.EXTERNAL_SENSOR_MESSAGE);

        if (externalSensorMessage == null) {
            // 변환할 MQTT DTO가 없으면 메시지를 건너뛴다.
            log.warn(
                    "[{}] 외부 센서 메시지가 없어 변환을 건너뜁니다: {}",
                    getId(),
                    message
            );
            return;
        }

        sensorTransformService.transform(externalSensorMessage)
                .forEach(this::sendSensor);
    }

    private void sendSensor(
            SensorPayload sensorPayload
    ) {
        String topic = String.format(
                "%d/%d/%d/%s/%s",
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sensorPayload.sectionId(),
                sanitize(sensorPayload.deviceEui()),
                sanitize(sensorPayload.sensorType())
        );

        send(OUTPUT_PORT, new Message(
                Map.of(
                        MessageFields.TOPIC, topic,
                        MessageFields.SENSOR_PAYLOAD, sensorPayload
                )
        ));
    }

    private String sanitize(String value) {
        // MQTT topic 구분자와 공백을 안전한 문자로 바꾼다.
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value.trim().replaceAll("[\\s/]+", "_");
    }
}
