package com.nhnacademy.ruleengine.engine.dto.sensor;

import com.nhnacademy.ruleengine.engine.domain.EnvironmentType;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 룰 엔진에서 사용하는 표준 센서 타입과 단위를 정의한다.
 */
public enum SensorType {
    TEMPERATURE("temperature", "C", EnvironmentType.TEMPERATURE),
    HUMIDITY("humidity", "%", EnvironmentType.HUMIDITY),
    DOOR("door", "문열림 여부", EnvironmentType.DOOR),
    ILLUMINATION("illumination", "lux", EnvironmentType.ILLUMINANCE);

    private final String value;
    private final String unit;
    // 인벤토리에 환경 이벤트를 보낼 때 쓰는 환경유형. 센서타입과 이름이 달라서(illumination -> ILLUMINANCE) 여기서 짝지어 둔다.
    private final EnvironmentType environmentType;

    SensorType(String value, String unit, EnvironmentType environmentType) {
        this.value = value;
        this.unit = unit;
        this.environmentType = environmentType;
    }

    public String value() {
        return value;
    }

    public String unit() {
        return unit;
    }

    public EnvironmentType environmentType() {
        return environmentType;
    }

    public static Optional<SensorType> findByValue(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalizedValue = value.trim().toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(sensorType -> sensorType.value.equals(normalizedValue))
                .findFirst();
    }
}
