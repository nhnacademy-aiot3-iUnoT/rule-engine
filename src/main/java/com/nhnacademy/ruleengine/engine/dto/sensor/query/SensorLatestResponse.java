package com.nhnacademy.ruleengine.engine.dto.sensor.query;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;

import java.util.Objects;

// 최신 센서 데이터 조회 API의 응답 형식이다.
public record SensorLatestResponse(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long zoneId,
        String sensorType,
        Double value,
        String unit,
        String measuredAt
) {
    public static SensorLatestResponse from(SensorPayload payload) {
        Objects.requireNonNull(payload, "센서 데이터는 필수입니다.");

        return new SensorLatestResponse(
                payload.organizationId(),
                payload.deviceEui(),
                payload.storageId(),
                payload.zoneId(),
                payload.sensorType(),
                payload.value(),
                payload.unit(),
                payload.time()
        );
    }
}
