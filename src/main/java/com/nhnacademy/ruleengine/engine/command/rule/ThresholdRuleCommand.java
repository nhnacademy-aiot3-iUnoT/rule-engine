package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

// 최소/최대 임계값으로 판단하는 센서(temperature, humidity, illumination 등)의 공통 룰 판단.
// ThresholdPolicyDto가 센서타입을 key로 하는 Map이라, 새 임계값 기반 센서타입이 추가돼도
// 이 클래스는 그대로 두고 정책 데이터(Map)에 항목만 추가하면 된다.
@Slf4j
@Component
@RequiredArgsConstructor
public class ThresholdRuleCommand implements EnvironmentRuleCommand {

    private static final Set<String> SUPPORTED_SENSOR_TYPES = Set.of(
            SensorType.TEMPERATURE.value(),
            SensorType.HUMIDITY.value(),
            SensorType.ILLUMINATION.value()
    );

    private final ThresholdPolicyService thresholdPolicyService;

    @Override
    public boolean supports(String sensorType) {
        return SUPPORTED_SENSOR_TYPES.contains(sensorType);
    }

    @Override
    public Optional<RuleResultDto> evaluate(SensorPayload sensorPayload) {
        ThresholdPolicyDto thresholdPolicy = thresholdPolicyService.getThresholdPolicy(
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sensorPayload.sectionId()
        );

        String sensorType = sensorPayload.sensorType();
        Optional<ThresholdPolicyDto.ThresholdRange> range = thresholdPolicy.rangeFor(sensorType);

        if (range.isEmpty()) {
            log.info(
                    "[{}] 임계값 설정이 없어 검사를 건너뜁니다. organizationId={}, storageId={}, sectionId={}, sensorType={}, deviceEui={}",
                    getClass().getSimpleName(),
                    sensorPayload.organizationId(),
                    sensorPayload.storageId(),
                    sensorPayload.sectionId(),
                    sensorType,
                    sensorPayload.deviceEui()
            );
            return Optional.empty();
        }

        return Optional.of(checkValue(sensorPayload, range.get()));
    }

    private RuleResultDto checkValue(
            SensorPayload sensorPayload,
            ThresholdPolicyDto.ThresholdRange range
    ) {
        Double value = sensorPayload.value();
        String sensorType = sensorPayload.sensorType();
        Double min = range.min();
        Double max = range.max();
        Integer alertDurationMinutes = range.alertDurationMinutes();

        if (min != null && value < min) {
            return build(sensorPayload, ViolationType.BELOW_MIN, true, min, max, alertDurationMinutes, sensorType + ": 최솟값 미달");
        }

        if (max != null && value > max) {
            return build(sensorPayload, ViolationType.ABOVE_MAX, true, min, max, alertDurationMinutes, sensorType + ": 최댓값 초과");
        }

        return build(sensorPayload, ViolationType.NORMAL, false, min, max, alertDurationMinutes, sensorType + ": 정상");
    }

    private RuleResultDto build(
            SensorPayload sensorPayload,
            ViolationType violationType,
            boolean violated,
            Double min,
            Double max,
            Integer thresholdDurationMinutes,
            String message
    ) {
        return RuleResultDto.fromThreshold(RuleResultCreateRequest.ofThreshold(
                sensorPayload, violationType, violated, min, max, thresholdDurationMinutes, message
        ));
    }
}
