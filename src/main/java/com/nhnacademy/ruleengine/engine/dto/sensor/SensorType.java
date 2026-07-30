package com.nhnacademy.ruleengine.engine.dto.sensor;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * 룰 엔진에서 사용하는 표준 센서 타입과 단위를 정의한다.
 */
public enum SensorType {
    TEMPERATURE("temperature", "C"),
    HUMIDITY("humidity", "%"),
    DOOR("door", "bool"),
    ILLUMINATION("illumination", "lux");

    private final String value;
    private final String unit;

    SensorType(String value, String unit) {
        this.value = value;
        this.unit = unit;
    }

    public String value() {
        return value;
    }

    public String unit() {
        return unit;
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
