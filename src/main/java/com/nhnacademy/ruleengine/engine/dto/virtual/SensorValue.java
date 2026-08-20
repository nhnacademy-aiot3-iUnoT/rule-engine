package com.nhnacademy.ruleengine.engine.dto.virtual;

import java.util.Objects;

public record SensorValue(

        GenerationMode mode,

        Double min,

        Double max,

        Double fixedValue,

        Double probability

) {
    public SensorValue {
        Objects.requireNonNull(mode, "생성 모드는 필수입니다.");

        switch (mode) {
            case FIXED -> requireFinite(fixedValue, "고정값");
            case RANGE -> {
                requireFinite(min, "최솟값");
                requireFinite(max, "최댓값");

                if (min > max) {
                    throw new IllegalArgumentException(
                            "최솟값은 최댓값보다 클 수 없습니다."
                    );
                }
            }
            case PROBABILITY -> {
                requireFinite(probability, "확률");

                if (probability < 0.0 || probability > 1.0) {
                    throw new IllegalArgumentException(
                            "확률은 0 이상 1 이하여야 합니다."
                    );
                }
            }
        }
    }

    private static void requireFinite(Double value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(fieldName + "은(는) 유한한 숫자여야 합니다.");
        }
    }
}
