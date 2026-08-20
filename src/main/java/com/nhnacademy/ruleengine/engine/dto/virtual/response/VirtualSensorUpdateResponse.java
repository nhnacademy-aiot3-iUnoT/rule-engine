package com.nhnacademy.ruleengine.engine.dto.virtual.response;


public record VirtualSensorUpdateResponse(
        boolean updated
) {
    public static VirtualSensorUpdateResponse from(boolean updated) {
        return new VirtualSensorUpdateResponse(updated);
    }

}