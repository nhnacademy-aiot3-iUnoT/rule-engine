package com.nhnacademy.ruleengine.engine.domain;

// 인벤토리의 환경 이벤트가 쓰는 환경유형. 값이 어긋나면 인벤토리가 요청을 거절하므로 그대로 맞춘다.
// 룰 엔진 내부 센서타입(SensorType)과는 이름이 달라서(illumination -> ILLUMINANCE) SensorType이 직접 들고 있다.
public enum EnvironmentType {
    TEMPERATURE, HUMIDITY, ILLUMINANCE, DOOR
}
