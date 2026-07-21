package com.nhnacademy.ruleengine.engine.dto;

public record LocationCreateRequest(
        String deviceName,
        String deviceEui,
        Integer measurementIntervalSeconds,
        SensorRangeRequest temperature,
        SensorRangeRequest humidity,
        Double doorOpenProbability

){
}
