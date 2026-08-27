package com.nhnacademy.ruleengine.engine.dto.virtual.response;

import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;


public record VirtualSensorInfoResponse(

        boolean registered,

        String deviceEui,

        Long measurementIntervalSeconds,

        VirtualSensorValues virtualSensorValues,

        VirtualSensorStatus status,

        // 구역 센서로 등록되기 전에는 null이다. 이때 만들어진 데이터는 갈 곳이 없어 버려진다.
        Long zoneId
) {
    public static VirtualSensorInfoResponse from(
            VirtualSensorConfig config,
            boolean active,
            Long zoneId
    ) {
        return new VirtualSensorInfoResponse(
                true,
                config.deviceEui(),
                config.measurementIntervalSeconds(),
                config.virtualSensorValues(),
                active ? VirtualSensorStatus.ACTIVE : VirtualSensorStatus.INACTIVE,
                zoneId
        );
    }

    public static VirtualSensorInfoResponse notRegistered() {
        return new VirtualSensorInfoResponse(false, null, null, null, null, null);
    }
}
