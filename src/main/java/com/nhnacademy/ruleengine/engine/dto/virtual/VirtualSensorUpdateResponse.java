package com.nhnacademy.ruleengine.engine.dto.virtual;


public record VirtualSensorUpdateResponse(
        boolean updated
) {
    public static VirtualSensorUpdateResponse from(boolean updated) {
        return new VirtualSensorUpdateResponse(updated);
    }

}