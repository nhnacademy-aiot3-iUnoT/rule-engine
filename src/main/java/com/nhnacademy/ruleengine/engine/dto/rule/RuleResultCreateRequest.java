package com.nhnacademy.ruleengine.engine.dto.rule;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;

public record RuleResultCreateRequest(
        SensorPayload sensorPayload,
        ViolationType violationType,
        boolean violated,
        Double min,
        Double max,
        Integer thresholdDurationMinutes,
        String message
) {
    public static RuleResultCreateRequest ofThreshold(
            SensorPayload sensorPayload,
            ViolationType violationType,
            boolean violated,
            Double min,
            Double max,
            Integer thresholdDurationMinutes,
            String message
    ){
        return new RuleResultCreateRequest(sensorPayload, violationType, violated, min, max, thresholdDurationMinutes, message);
    }

    public static RuleResultCreateRequest ofDoorState(
            SensorPayload sensorPayload,
            ViolationType violationType,
            boolean violated,
            String message
    ){
        return new RuleResultCreateRequest(sensorPayload, violationType, violated, null, null, null, message);
    }
}
