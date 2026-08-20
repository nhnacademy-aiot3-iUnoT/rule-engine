package com.nhnacademy.ruleengine.engine.dto.virtual.response;

import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;

public record VirtualSensorInfoResponse(

        String deviceEui,

        Long measurementIntervalSeconds,

        VirtualSensorValues virtualSensorValues,

        VirtualSensorStatus status
) {
    public static VirtualSensorInfoResponse from(VirtualSensorConfig config, boolean active) {
        return new VirtualSensorInfoResponse(
                config.deviceEui(),
                config.measurementIntervalSeconds(),
                config.virtualSensorValues(),
                active ? VirtualSensorStatus.ACTIVE : VirtualSensorStatus.INACTIVE
        );
    }
}
