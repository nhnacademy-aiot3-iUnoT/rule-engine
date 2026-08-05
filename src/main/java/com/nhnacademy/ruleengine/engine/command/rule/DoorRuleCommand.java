package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 문 센서(open/close)의 룰 판단. 임계값 정책과 무관하게 즉시 판단한다.
@Slf4j
@Component
public class DoorRuleCommand implements EnvironmentRuleCommand {

    @Override
    public boolean supports(String sensorType) {
        return SensorType.DOOR.value().equals(sensorType);
    }

    @Override
    public Optional<RuleResultDto> evaluate(SensorPayload sensorPayload) {
        double value = sensorPayload.value();

        if (value == 1.0) {
            return Optional.of(RuleResultDto.fromDoorState(
                    RuleResultCreateRequest.ofDoorState(sensorPayload, ViolationType.OPEN, true, "door: 열림")
            ));
        }

        if (value == 0.0) {
            return Optional.of(RuleResultDto.fromDoorState(
                    RuleResultCreateRequest.ofDoorState(sensorPayload, ViolationType.CLOSED, false, "door: 닫힘")
            ));
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
}
