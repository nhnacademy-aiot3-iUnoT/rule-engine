package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;

import java.util.Map;
import java.util.Objects;


public record VirtualSensorValues(
        Map<SensorType, SensorValue> valueMap
) {
    public VirtualSensorValues {
        Objects.requireNonNull(valueMap, "센서 설정은 필수입니다.");

        if (valueMap.isEmpty()) {
            throw new IllegalArgumentException("센서를 하나 이상 설정해야 합니다.");
        }

        valueMap.forEach(VirtualSensorValues::validateMode);
        valueMap = Map.copyOf(valueMap);
    }

    private static void validateMode(SensorType sensorType, SensorValue sensorValue) {
        Objects.requireNonNull(sensorType, "센서 타입은 필수입니다.");
        Objects.requireNonNull(sensorValue, "센서 값 설정은 필수입니다.");

        GenerationMode mode = Objects.requireNonNull(
                sensorValue.mode(),
                "생성 모드는 필수입니다."
        );

        if (sensorType == SensorType.DOOR) {
            validateDoorMode(sensorValue, mode);
            return;
        }

        if (mode == GenerationMode.PROBABILITY) {
            throw new IllegalArgumentException(
                    sensorType + " 센서는 PROBABILITY 모드를 사용할 수 없습니다."
            );
        }
    }

    private static void validateDoorMode(SensorValue sensorValue, GenerationMode mode) {
        if (mode != GenerationMode.FIXED && mode != GenerationMode.PROBABILITY) {
            throw new IllegalArgumentException(
                    "문 센서는 FIXED 또는 PROBABILITY 모드만 사용할 수 있습니다."
            );
        }

        if (mode == GenerationMode.FIXED
                && Double.compare(sensorValue.fixedValue(), 0.0) != 0
                && Double.compare(sensorValue.fixedValue(), 1.0) != 0) {
            throw new IllegalArgumentException(
                    "문 센서의 고정값은 0 또는 1이어야 합니다."
            );
        }
    }
}
