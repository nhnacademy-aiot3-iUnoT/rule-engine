package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.ZoneResolver;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

/**
 * deviceEui로 센서가 설치된 위치(조직/저장소/구역)를 채워 넣는다.
 * <p>
 * 아직 구역에 등록되지 않았거나 구역/저장소가 비활성인 기기의 데이터는 갈 곳이 없으므로 버린다.
 * 구역에 등록되거나 다시 활성이 되면 조회 캐시가 만료되는 대로 저절로 다시 흐른다.
 */
@Slf4j
public class SensorZoneResolveNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final ZoneResolver zoneResolver;

    public SensorZoneResolveNode(String id, ZoneResolver zoneResolver) {
        super(id);

        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        this.zoneResolver = zoneResolver;
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayload payload = message.get(MessageFields.SENSOR_PAYLOAD);

        if (payload == null) {
            log.warn("[{}] 센서 데이터가 없습니다", getId());
            return;
        }

        Optional<ResolvedZoneResponse> resolvedZone = zoneResolver.resolveActive(payload.deviceEui());

        if (resolvedZone.isEmpty()) {
            log.debug(
                    "[{}] 구역에 등록되지 않았거나 비활성 구역의 기기라 데이터를 버립니다. deviceEui={}",
                    getId(),
                    payload.deviceEui()
            );

            return;
        }

        ResolvedZoneResponse zone = resolvedZone.get();

        SensorPayload located = new SensorPayload(
                zone.organizationId(),
                payload.deviceEui(),
                zone.storageId(),
                zone.zoneId(),
                payload.sensorType(),
                payload.value(),
                payload.unit(),
                payload.time()
        );

        send(
                OUTPUT_PORT,
                new Message(Map.of(
                        MessageFields.TOPIC, topicOf(located),
                        MessageFields.SENSOR_PAYLOAD, located
                ))
        );
    }

    private String topicOf(SensorPayload payload) {
        return String.format(
                "%d/%d/%d/%s/%s",
                payload.organizationId(),
                payload.storageId(),
                payload.zoneId(),
                sanitize(payload.deviceEui()),
                sanitize(payload.sensorType())
        );
    }

    private String sanitize(String value) {
        return value.trim().replaceAll("[\\s/]+", "_");
    }
}
