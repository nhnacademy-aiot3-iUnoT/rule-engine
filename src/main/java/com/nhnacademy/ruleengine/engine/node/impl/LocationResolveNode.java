package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class LocationResolveNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final String SENSOR_PAYLOAD_KEY = "sensorPayload";
    private static final Map<String, Long> LOCATION_IDS = Map.of(
            "사무실", 1L,
            "실습실", 2L,
            "사무실 밖", 3L
    );

    public LocationResolveNode(String id) {
        super(id);

        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayloadDto sensorPayload = message.get(SENSOR_PAYLOAD_KEY);
        if (sensorPayload == null) {
            log.warn("[{}] sensorPayload가 없어 위치 조회를 건너뜁니다.", getId());
            return;
        }

        // TODO 장소 관리 서비스 연동 후 applicationName + location으로 조회한다.
        Long locationId = LOCATION_IDS.get(sensorPayload.location());
        if (locationId == null) {
            log.warn(
                    "[{}] 등록되지 않은 위치입니다. applicationName={}, location={}",
                    getId(),
                    sensorPayload.applicationName(),
                    sensorPayload.location()
            );
            return;
        }

        SensorPayloadDto resolvedPayload = sensorPayload.withLocationId(locationId);
        send(
                OUTPUT_PORT,
                message.withEntry(SENSOR_PAYLOAD_KEY, resolvedPayload)
        );
    }
}
