package com.nhnacademy.ruleengine.engine.dto.virtual;

public record VirtualSensorCreateRequest(
        String deviceName,
        String deviceEui,
        Integer measurementIntervalSeconds,
        SensorValueRange temperature,
        SensorValueRange humidity,
        Double doorOpenProbability

){
}
