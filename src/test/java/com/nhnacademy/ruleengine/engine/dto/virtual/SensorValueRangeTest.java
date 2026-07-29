package com.nhnacademy.ruleengine.engine.dto.virtual;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SensorValueRangeTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    @DisplayName("정상 범위이면 생성된다")
    void createWithValidRange() {
        SensorValueRange range = new SensorValueRange(0.0, 100.0);

        assertAll(
                () -> assertEquals(0.0, range.min()),
                () -> assertEquals(100.0, range.max())
        );
    }

    @Test
    @DisplayName("최솟값이 최댓값보다 크면 예외가 발생한다")
    void throwExceptionWhenMinIsGreaterThanMax() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new SensorValueRange(100.0, 0.0)
        );

        assertEquals(
                "최솟값은 최댓값보다 클 수 없습니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("최솟값이 null이면 검증에 실패한다")
    void failValidationWhenMinIsNull() {
        SensorValueRange range = new SensorValueRange(null, 100.0);

        Set<ConstraintViolation<SensorValueRange>> violations =
                validator.validate(range);

        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals(
                        "최솟값은 필수입니다.",
                        violations.iterator().next().getMessage()
                )
        );
    }

    @Test
    @DisplayName("최댓값이 null이면 검증에 실패한다")
    void failValidationWhenMaxIsNull() {
        SensorValueRange range = new SensorValueRange(0.0, null);

        Set<ConstraintViolation<SensorValueRange>> violations =
                validator.validate(range);

        assertAll(
                () -> assertEquals(1, violations.size()),
                () -> assertEquals(
                        "최댓값은 필수입니다.",
                        violations.iterator().next().getMessage()
                )
        );
    }
}