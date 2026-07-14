package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.SensorContextDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;

// 센서 타입별 측정값 변환 전략의 공통 규약이다.
public interface SensorCommand {
    String getSensorType();

    SensorPayloadDto execute(Object value, SensorContextDto sensorContextDto);
}
