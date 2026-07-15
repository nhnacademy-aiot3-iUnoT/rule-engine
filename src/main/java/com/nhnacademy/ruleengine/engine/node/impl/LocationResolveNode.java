package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.location.LocationCatalog;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocationResolveNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final String SENSOR_PAYLOAD_KEY = "sensorPayload";
    private final LocationCatalog locationCatalog;

    public LocationResolveNode(String id, LocationCatalog locationCatalog) {
        super(id);
        this.locationCatalog = locationCatalog;

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

        Long locationId = locationCatalog.resolve(
                        sensorPayload.applicationName(),
                        sensorPayload.location()
                )
                .orElse(null);
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
