package com.nhnacademy.ruleengine.engine.dto;

public record RuleResultCreateRequest(
        SensorPayloadDto sensorPayload,
        boolean violated,
        Double min,
        Double max,
        Integer thresholdDurationMinutes,
        String message
) {
    public static RuleResultCreateRequest ofThreshold(
            SensorPayloadDto sensorPayload,
            boolean violated,
            Double min,
            Double max,
            Integer thresholdDurationMinutes,
            String message
    ){
        return new RuleResultCreateRequest(sensorPayload, violated, min, max, thresholdDurationMinutes, message);
    }

    public static RuleResultCreateRequest ofDoorState(
            SensorPayloadDto sensorPayload,
            boolean violated,
            String message
    ){
        return new RuleResultCreateRequest(sensorPayload, violated, null, null, null, message);
    }
}
