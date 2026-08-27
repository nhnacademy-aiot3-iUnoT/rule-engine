package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorUpdateRequest;

import java.util.Objects;

// 가상 센서 Flow 실행에 필요한 설정을 명확한 타입으로 전달한다.
// 어느 구역에서 측정되는지는 설정에 담지 않는다. 실제 센서와 똑같이 deviceEui를 구역에 등록해야
// 위치가 정해지고, 위치는 데이터를 만들 때마다 인벤토리에서 조회한다.
public record VirtualSensorConfig(
        Long organizationId,
        String deviceEui,
        VirtualSensorValues virtualSensorValues,
        Long measurementIntervalSeconds
) {
    public VirtualSensorConfig {
        requirePositive(organizationId, "organizationId");

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
            VirtualSensorCreateRequest request
    ) {
        Objects.requireNonNull(request, "가상 센서 설정은 필수입니다.");

        return new VirtualSensorConfig(
                organizationId,
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
                existing.deviceEui(),
                request.virtualSensorValues(),
                request.measurementIntervalSeconds() != null ? request.measurementIntervalSeconds() : existing.measurementIntervalSeconds()
        );
    }

    public boolean ownedBy(Long organizationId) {
        return this.organizationId != null && this.organizationId.equals(organizationId);
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }
}
