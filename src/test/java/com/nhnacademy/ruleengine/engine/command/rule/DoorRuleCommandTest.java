package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DoorRuleCommandTest {

    private final DoorRuleCommand command = new DoorRuleCommand();

    @Test
    @DisplayName("문 센서 타입인 경우 true를 반환한다")
    void supportsTrue() {
        // when
        boolean result = command.supports(
                SensorType.DOOR.value()
        );

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("문 센서가 아닌 경우 false를 반환한다")
    void supportsFalse() {
        // when
        boolean result = command.supports(
                SensorType.TEMPERATURE.value()
        );

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("문이 열릴 경우 열림 상태를 반환한다")
    void evaluateDoorOpen() {
        // given
        SensorPayload payload = createSensorPayload(1.0);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(ViolationType.OPEN, result.get().violationType())
        );

    }

    @Test
    @DisplayName("문이 닫힐 경우 닫힘 상태를 반환한다")
    void evaluateDoorClose() {
        // given
        SensorPayload payload = createSensorPayload(0.0);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertAll(
                () -> assertTrue(result.isPresent()),
                () -> assertEquals(ViolationType.CLOSED, result.get().violationType())
        );
    }

    @Test
    @DisplayName("지원하지 않는 문 상태 값인 경우 빈 결과를 반환한다")
    void evaluateUnsupportedValue() {
        // given
        SensorPayload payload = createSensorPayload(2.0);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertTrue(result.isEmpty());
    }

    private SensorPayload createSensorPayload(Double value) {
        return new SensorPayload(
                1L,
                "test-eui",
                1L,
                1L,
                SensorType.DOOR.value(),
                value,
                SensorType.DOOR.unit(),
                "time"
        );
    }
}