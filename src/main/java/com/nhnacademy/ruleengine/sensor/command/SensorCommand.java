package com.nhnacademy.ruleengine.sensor.command;

import com.nhnacademy.ruleengine.sensor.dto.SensorContext;
import com.nhnacademy.ruleengine.sensor.dto.SensorPayloadDto;

// 센서 타입별 측정값 변환 전략의 공통 규약이다.
public interface SensorCommand {
    String getSensorType();

    SensorPayloadDto execute(Object value, SensorContext sensorContext);
}
