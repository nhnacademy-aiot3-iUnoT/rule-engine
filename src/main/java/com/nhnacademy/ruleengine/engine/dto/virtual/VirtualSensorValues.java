package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;

import java.util.Map;


public record VirtualSensorValues(
        Map<SensorType, SensorValue> valueMap
) {
}
