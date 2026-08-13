package com.nhnacademy.ruleengine.engine.validation;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorPayloadValidatorTest {

    @Test
    @DisplayName("유효한 센서 데이터인 경우 예외가 발생하지 않는다")
    void validateSuccess() {
        // given
        SensorPayload payload = createValidPayload();

        // when & then
        assertDoesNotThrow(
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("organizationId가 null이면 예외가 발생한다")
    void validateOrganizationIdNull() {
        SensorPayload payload = new SensorPayload(
                null,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );

        assertEquals(
                "organizationId는 양수여야 합니다.",
                exception.getMessage()
        );
    }

    @Test
    @DisplayName("organizationId가 0이면 예외가 발생한다")
    void validateOrganizationIdZero() {
        SensorPayload payload = createPayload(
                0L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("organizationId가 음수이면 예외가 발생한다")
    void validateOrganizationIdNegative() {
        SensorPayload payload = createPayload(
                -1L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("deviceEui가 null이면 예외가 발생한다")
    void validateDeviceEuiNull() {
        SensorPayload payload = createPayload(
                1L,
                null,
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("deviceEui가 빈 문자열이면 예외가 발생한다")
    void validateDeviceEuiBlank() {
        SensorPayload payload = createPayload(
                1L,
                " ",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("storageId가 null이면 예외가 발생한다")
    void validateStorageIdNull() {
        SensorPayload payload = createPayload(
                1L,
                "test-eui",
                null,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("sectionId가 null이면 예외가 발생한다")
    void validateSectionIdNull() {
        SensorPayload payload = createPayload(
                1L,
                "test-eui",
                1L,
                null,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("sensorType이 null이면 예외가 발생한다")
    void validateSensorTypeNull() {
        SensorPayload payload = createPayload(
                1L,
                "test-eui",
                1L,
                1L,
                null,
                25.0,
                "C",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("unit이 빈 문자열이면 예외가 발생한다")
    void validateUnitBlank() {
        SensorPayload payload = createPayload(
                1L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                " ",
                "time"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("time이 null이면 예외가 발생한다")
    void validateTimeNull() {
        SensorPayload payload = createPayload(
                1L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                null
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );
    }

    @Test
    @DisplayName("value가 null이면 예외가 발생한다")
    void validateValueNull() {
        SensorPayload payload = createPayload(
                1L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                null,
                "C",
                "time"
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> SensorPayloadValidator.validate(payload)
        );

        assertEquals(
                "value는 필수입니다.",
                exception.getMessage()
        );
    }

    private SensorPayload createValidPayload() {
        return createPayload(
                1L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                25.0,
                "C",
                "time"
        );
    }

    private SensorPayload createPayload(
            Long organizationId,
            String deviceEui,
            Long storageId,
            Long sectionId,
            String sensorType,
            Double value,
            String unit,
            String time
    ) {
        return new SensorPayload(
                organizationId,
                deviceEui,
                storageId,
                sectionId,
                sensorType,
                value,
                unit,
                time
        );
    }
}