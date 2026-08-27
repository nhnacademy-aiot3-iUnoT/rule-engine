package com.nhnacademy.ruleengine.engine.dto.virtual.response;


public record VirtualSensorCreateResponse(
        String deviceEui

) {
    public static VirtualSensorCreateResponse from(String deviceEui) {
        return new VirtualSensorCreateResponse(deviceEui);
    }
}
