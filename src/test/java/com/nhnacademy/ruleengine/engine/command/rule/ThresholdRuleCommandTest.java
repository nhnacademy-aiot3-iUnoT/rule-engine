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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThresholdRuleCommandTest {

    @Mock
    private ThresholdPolicyService thresholdPolicyService;

    @InjectMocks
    private ThresholdRuleCommand command;

    @Test
    @DisplayName("판단한 센서타입의 임계시간을 결과에 담는다")
    void useAlertDurationOfMatchedSensorType() {
        // given
        stubPolicy();

        // when
        RuleResultDto result = command.evaluate(payload(SensorType.HUMIDITY, 80.0)).orElseThrow();

        // then
        assertAll(
                () -> assertEquals(ViolationType.ABOVE_MAX, result.violationType()),
                () -> assertTrue(result.violated()),
                // 온도(5분)가 아니라 습도에 설정된 10분이어야 한다.
                () -> assertEquals(10, result.durationMinutes())
        );
    }

    @Test
    @DisplayName("최솟값 미만이면 위반으로 판단한다")
    void evaluateBelowMin() {
        // given
        stubPolicy();

        // when
        RuleResultDto result = command.evaluate(payload(SensorType.TEMPERATURE, 19.0)).orElseThrow();

        // then
        assertAll(
                () -> assertEquals(ViolationType.BELOW_MIN, result.violationType()),
                () -> assertEquals(5, result.durationMinutes())
        );
    }

    @Test
    @DisplayName("범위 이내면 정상으로 판단한다")
    void evaluateNormal() {
        // given
        stubPolicy();

        // when
        RuleResultDto result = command.evaluate(payload(SensorType.TEMPERATURE, 25.0)).orElseThrow();

        // then
        assertEquals(ViolationType.NORMAL, result.violationType());
    }

    @Test
    @DisplayName("해당 센서의 임계값 설정이 없으면 빈 결과를 반환한다")
    void evaluateWithoutThreshold() {
        // given
        when(thresholdPolicyService.getThresholdPolicy(3L))
                .thenReturn(new ThresholdPolicyDto(Map.of()));

        // when
        Optional<RuleResultDto> result = command.evaluate(payload(SensorType.TEMPERATURE, 25.0));

        // then
        assertTrue(result.isEmpty());
    }

    private void stubPolicy() {
        when(thresholdPolicyService.getThresholdPolicy(3L)).thenReturn(
                new ThresholdPolicyDto(Map.of(
                        SensorType.TEMPERATURE.value(), new ThresholdRange(20.0, 30.0, 5),
                        SensorType.HUMIDITY.value(), new ThresholdRange(30.0, 70.0, 10)
                ))
        );
    }

    private SensorPayload payload(SensorType sensorType, Double value) {
        return new SensorPayload(
                1L,
                "test-eui",
                2L,
                3L,
                sensorType.value(),
                value,
                sensorType.unit(),
                "2026-08-14T00:00:00Z"
        );
    }
}
