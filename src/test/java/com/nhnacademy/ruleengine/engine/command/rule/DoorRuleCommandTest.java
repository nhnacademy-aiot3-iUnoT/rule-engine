package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoorRuleCommandTest {

    @Mock
    private ThresholdPolicyService thresholdPolicyService;

    @InjectMocks
    private DoorRuleCommand command;

    @Test
    @DisplayName("door 센서타입만 처리한다")
    void supportsDoorOnly() {
        assertAll(
                () -> assertTrue(command.supports(SensorType.DOOR.value())),
                () -> assertFalse(command.supports(SensorType.TEMPERATURE.value()))
        );
    }

    @Test
    @DisplayName("열림이면 임계시간 없이 즉시 위반으로 판단한다")
    void evaluateOpen() {
        // given
        stubPolicy();

        // when
        RuleResultDto result = command.evaluate(payload(1.0)).orElseThrow();

        // then
        assertAll(
                () -> assertEquals(ViolationType.OPEN, result.violationType()),
                () -> assertTrue(result.violated()),
                // 임계시간이 없어야 EnvironmentStatusDecisionNode가 WARNING을 거치지 않고 바로 CRITICAL로 올린다.
                () -> assertNull(result.durationMinutes())
        );
    }

    @Test
    @DisplayName("닫힘이면 정상으로 판단한다")
    void evaluateClosed() {
        // given
        stubPolicy();

        // when
        RuleResultDto result = command.evaluate(payload(0.0)).orElseThrow();

        // then
        assertAll(
                () -> assertEquals(ViolationType.CLOSED, result.violationType()),
                () -> assertFalse(result.violated())
        );
    }

    @Test
    @DisplayName("문열림 룰 설정이 없으면 빈 결과를 반환해 감시하지 않는다")
    void evaluateWithoutDoorThreshold() {
        // given: 다른 센서타입 설정만 있고 문 설정은 없는 구역
        when(thresholdPolicyService.getThresholdPolicy(1L, 2L, 3L)).thenReturn(
                new ThresholdPolicyDto(1L, 2L, 3L, Map.of(
                        SensorType.TEMPERATURE.value(), new ThresholdRange(20.0, 30.0, 5)
                ))
        );

        // when
        Optional<RuleResultDto> result = command.evaluate(payload(1.0));

        // then
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("지원하지 않는 문 상태 값이면 빈 결과를 반환한다")
    void evaluateUnsupportedValue() {
        // given
        stubPolicy();

        // when
        Optional<RuleResultDto> result = command.evaluate(payload(2.0));

        // then
        assertTrue(result.isEmpty());
    }

    private void stubPolicy() {
        when(thresholdPolicyService.getThresholdPolicy(1L, 2L, 3L)).thenReturn(
                new ThresholdPolicyDto(1L, 2L, 3L, Map.of(
                        SensorType.DOOR.value(), new ThresholdRange(null, null, null)
                ))
        );
    }

    private SensorPayload payload(Double value) {
        return new SensorPayload(
                1L,
                "test-eui",
                2L,
                3L,
                SensorType.DOOR.value(),
                value,
                SensorType.DOOR.unit(),
                "2026-08-14T00:00:00Z"
        );
    }
}
