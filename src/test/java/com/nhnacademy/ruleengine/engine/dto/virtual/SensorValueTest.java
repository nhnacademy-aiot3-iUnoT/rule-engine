package com.nhnacademy.ruleengine.engine.dto.virtual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorValueTest {

    @Test
    @DisplayName("모드별 필수 값이 있으면 생성된다")
    void createWithValidModeValue() {
        assertDoesNotThrow(() -> new SensorValue(
                GenerationMode.FIXED,
                null,
                null,
                10.0,
                null
        ));
        assertDoesNotThrow(() -> new SensorValue(
                GenerationMode.RANGE,
                0.0,
                100.0,
                null,
                null
        ));
        assertDoesNotThrow(() -> new SensorValue(
                GenerationMode.PROBABILITY,
                null,
                null,
                null,
                0.5
        ));
    }

    @Test
    @DisplayName("범위 최솟값이 최댓값보다 크면 예외가 발생한다")
    void rejectReversedRange() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(
                        GenerationMode.RANGE,
                        100.0,
                        0.0,
                        null,
                        null
                )
        );

        assertEquals("최솟값은 최댓값보다 클 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("모드에 필요한 값이 없으면 예외가 발생한다")
    void rejectMissingModeValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.FIXED, null, null, null, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.RANGE, null, 10.0, null, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.PROBABILITY, null, null, null, null)
        );
    }

    @Test
    @DisplayName("확률이 0에서 1 사이가 아니면 예외가 발생한다")
    void rejectInvalidProbability() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.PROBABILITY, null, null, null, -0.1)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.PROBABILITY, null, null, null, 1.1)
        );
    }

    @Test
    @DisplayName("NaN과 무한대는 허용하지 않는다")
    void rejectNonFiniteValue() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.FIXED, null, null, Double.NaN, null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValue(GenerationMode.RANGE, 0.0, Double.POSITIVE_INFINITY, null, null)
        );
    }
}
