package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatabaseSaveNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private final SensorInfluxService influxService;

    public DatabaseSaveNode(String id, SensorInfluxService influxService) {
        super(id);

        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        this.influxService = influxService;

    }

    @Override
    protected void onProcess(Message message) {
        SensorPayload sensorPayload = message.get(MessageFields.SENSOR_PAYLOAD);

        if (sensorPayload == null) {
            log.warn("[{}] sensorPayload가 없어 저장을 건너뜁니다.", getId());
            return;
        }

        influxService.save(sensorPayload);
        log.debug("[{}] sensorPayload가 성공적으로 저장되었습니다.", getId());

        send(OUTPUT_PORT, message);
    }
}
