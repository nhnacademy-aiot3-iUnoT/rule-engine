package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThresholdRuleCommandTest {

    @Mock
    private ThresholdPolicyService service;

    @InjectMocks
    private ThresholdRuleCommand command;

    @Test
    @DisplayName("지원하는 종류의 타입일 경우 true를 반환한다")
    void supportsTrue() {
        // when
        boolean result = command.supports(
                SensorType.TEMPERATURE.value()
        );

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("지원하지 않는 종류의 타입인 경우 false를 반환한다")
    void supportsFalse() {
        // when
        boolean result = command.supports(SensorType.DOOR.value());

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("최솟값 미만인 경우 위반 결과를 반환한다")
    void evaluateBelowMin() {
        // given
        ThresholdPolicyDto policy = createPolicy(
                20.0,
                25.0
        );

        SensorPayload payload = createSensorPayload(19.0);

        when(service.getThresholdPolicy(
                payload.organizationId(),
                payload.storageId(),
                payload.sectionId()
        )).thenReturn(policy);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("최댓값을 초과하는 경우 위반 결과를 반환한다")
    void evaluateAboveMax() {
        // given
        ThresholdPolicyDto policy = createPolicy(
                20.0,
                25.0
        );

        SensorPayload payload = createSensorPayload(25.5);

        when(service.getThresholdPolicy(
                payload.organizationId(),
                payload.storageId(),
                payload.sectionId()
        )).thenReturn(policy);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("임계값 범위 이내인 경우 정상 결과를 반환한다")
    void evaluateNormal() {
        // given
        ThresholdPolicyDto policy = createPolicy(
                20.0,
                25.0
        );

        SensorPayload payload = createSensorPayload(22.0);

        when(service.getThresholdPolicy(
                payload.organizationId(),
                payload.storageId(),
                payload.sectionId()
        )).thenReturn(policy);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertTrue(result.isPresent());
    }

    @Test
    @DisplayName("해당 센서의 임계값 설정이 없는 경우 빈 결과를 반환한다")
    void evaluateWithoutThreshold() {
        // given
        ThresholdPolicyDto policy = new ThresholdPolicyDto(
                1L,
                1L,
                1L,
                new HashMap<>(),
                5
        );

        SensorPayload payload = createSensorPayload(25.5);

        when(service.getThresholdPolicy(
                payload.organizationId(),
                payload.storageId(),
                payload.sectionId()
        )).thenReturn(policy);

        // when
        Optional<RuleResultDto> result = command.evaluate(payload);

        // then
        assertTrue(result.isEmpty());
    }

    private ThresholdPolicyDto createPolicy(
            Double min,
            Double max
    ) {
        Map<String, ThresholdRange> ranges = new HashMap<>();

        ranges.put(
                SensorType.TEMPERATURE.value(),
                new ThresholdRange(min, max)
        );

        return new ThresholdPolicyDto(
                1L,
                1L,
                1L,
                ranges,
                5
        );
    }

    private SensorPayload createSensorPayload(Double value) {
        return new SensorPayload(
                1L,
                "test-eui",
                1L,
                1L,
                SensorType.TEMPERATURE.value(),
                value,
                SensorType.DOOR.unit(),
                "time"
        );
    }
}