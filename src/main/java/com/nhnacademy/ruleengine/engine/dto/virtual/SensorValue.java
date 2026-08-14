package com.nhnacademy.ruleengine.engine.dto.virtual;

public record SensorValue(

        GenerationMode mode,

        Double min,

        Double max,

        Double fixedValue,

        Double probability

) {
    public SensorValue {
        if (mode == GenerationMode.RANGE
                && (min != null && max != null)
                && min > max)
        {
            throw new IllegalArgumentException(
                    "최솟값은 최댓값보다 클 수 없습니다."
            );
        }

        if (mode == GenerationMode.PROBABILITY
                && probability != null
                && (probability >= 0.0 && probability <= 1.0 ))
        {
            throw new IllegalArgumentException(
                    "확률은 0 - 1 사이값이어야 합니다."
            );
        }
    }
}
