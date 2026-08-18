package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 문 센서(open/close)의 룰 판단로직
@Slf4j
@Component
@RequiredArgsConstructor
public class DoorRuleCommand implements EnvironmentRuleCommand {

    private final ThresholdPolicyService thresholdPolicyService;

    @Override
    public boolean supports(String sensorType) {
        return SensorType.DOOR.value().equals(sensorType);
    }

    // 구역에 문열림 룰이 설정된 경우에만 판단한다.
    @Override
    public Optional<RuleResultDto> evaluate(SensorPayload sensorPayload) {
        String sensorType = sensorPayload.sensorType();

        boolean configured = thresholdPolicyService.getThresholdPolicy(
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sensorPayload.sectionId()
        ).rangeFor(sensorType).isPresent();

        if (!configured) {
            log.info(
                    "[{}] 문열림 룰 설정이 없어 검사를 건너뜁니다. organizationId={}, storageId={}, sectionId={}, sensorType={}, deviceEui={}",
                    getClass().getSimpleName(),
                    sensorPayload.organizationId(),
                    sensorPayload.storageId(),
                    sensorPayload.sectionId(),
                    sensorType,
                    sensorPayload.deviceEui()
            );
            return Optional.empty();
        }

        return checkDoorState(sensorPayload);
    }

    private Optional<RuleResultDto> checkDoorState(SensorPayload sensorPayload) {
        double value = sensorPayload.value();

        if (value == 1.0) {
            return Optional.of(build(sensorPayload, ViolationType.OPEN, true, "door: 열림"));
        }

        if (value == 0.0) {
            return Optional.of(build(sensorPayload, ViolationType.CLOSED, false, "door: 닫힘"));
        }

        log.info(
                "지원하지 않는 문 상태 값입니다. organizationId={}, storageId={}, sectionId={}, deviceEui={}, value={}",
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sensorPayload.sectionId(),
                sensorPayload.deviceEui(),
                value
        );
        return Optional.empty();
    }

    private RuleResultDto build(
            SensorPayload sensorPayload,
            ViolationType violationType,
            boolean violated,
            String message
    ) {
        return RuleResultDto.fromDoorState(RuleResultCreateRequest.ofDoorState(
                sensorPayload, violationType, violated, message
        ));
    }
}
