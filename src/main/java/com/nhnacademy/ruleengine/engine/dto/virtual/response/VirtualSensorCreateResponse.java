package com.nhnacademy.ruleengine.engine.dto.virtual.response;


public record VirtualSensorCreateResponse(
        Long zoneId,
        String deviceEui

) {
    public static VirtualSensorCreateResponse from(Long zoneId, String deviceEui) {
        return new VirtualSensorCreateResponse(zoneId, deviceEui);
    }
}