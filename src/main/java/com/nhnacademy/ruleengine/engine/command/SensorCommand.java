package com.nhnacademy.ruleengine.engine.command;


import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;

// 센서 타입별 측정값 변환 전략의 공통 규약이다.
public interface SensorCommand {
    String getMeasurementKey();

    SensorType getSensorType();

    SensorPayload execute(Object value, SensorContext sensorContext);
}
