package com.nhnacademy.ruleengine.engine.dto;

import com.nhnacademy.ruleengine.engine.location.LocationCatalog.ResolvedLocation;

// 센서별 변환 로직에서 공통으로 사용하는 장치 정보를 담는다.
public record SensorContextDto(
        Long organizationId,
        String deviceEui,
        Long locationId,
        Long positionId,
        String time
) {

    public static SensorContextDto from(
            ExternalSensorMessageDto externalSensorMessage,
            ResolvedLocation resolvedLocation
    ) {
        if (resolvedLocation == null) {
            throw new IllegalArgumentException("변환된 위치 ID는 필수입니다.");
        }

        return new SensorContextDto(
                resolvedLocation.organizationId(),
                valueOrUnknown(externalSensorMessage.devEui()),
                resolvedLocation.locationId(),
                resolvedLocation.positionId(),
                valueOrUnknown(externalSensorMessage.time())
        );
    }

    private static String valueOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}
