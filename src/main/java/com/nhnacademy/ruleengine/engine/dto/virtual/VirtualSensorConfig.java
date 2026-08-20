package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorUpdateRequest;

import java.util.Objects;

// 가상 센서 Flow 실행에 필요한 설정을 명확한 타입으로 전달한다.
public record VirtualSensorConfig(
        Long organizationId,
        Long storageId,
        Long zoneId,
        String deviceEui,
        VirtualSensorValues virtualSensorValues,
        Long measurementIntervalSeconds
) {
    public VirtualSensorConfig {
        requirePositive(organizationId, "organizationId");
        requirePositive(storageId, "storageId");
        requirePositive(zoneId, "zoneId");

        if (deviceEui == null || deviceEui.isBlank()) {
            throw new IllegalArgumentException("deviceEui는 비어 있을 수 없습니다.");
        }

        Objects.requireNonNull(
                virtualSensorValues,
                "가상 센서 값 설정은 필수입니다."
        );

        if (measurementIntervalSeconds == null || measurementIntervalSeconds < 1) {
            throw new IllegalArgumentException("측정 주기는 1초 이상이어야 합니다.");
        }
    }

    public static VirtualSensorConfig from(
            Long organizationId,
            Long storageId,
            Long zoneId,
            VirtualSensorCreateRequest request
    ) {
        Objects.requireNonNull(request, "가상 센서 설정은 필수입니다.");

        return new VirtualSensorConfig(
                organizationId,
                storageId,
                zoneId,
                request.deviceEui(),
                request.virtualSensorValues(),
                request.measurementIntervalSeconds()
        );
    }

    public static VirtualSensorConfig merge(VirtualSensorConfig existing, VirtualSensorUpdateRequest request) {
        Objects.requireNonNull(existing, "기존 가상 센서 설정은 필수입니다.");
        Objects.requireNonNull(request, "변경할 가상 센서 설정은 필수입니다.");



        return new VirtualSensorConfig(
                existing.organizationId(),
                existing.storageId(),
                existing.zoneId(),
                existing.deviceEui(),
                request.virtualSensorValues() != null ? request.virtualSensorValues() : existing.virtualSensorValues(),
                request.measurementIntervalSeconds() != null ? request.measurementIntervalSeconds() : existing.measurementIntervalSeconds()
        );
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }
}
