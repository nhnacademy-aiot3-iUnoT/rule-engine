package com.nhnacademy.ruleengine.engine.dto;

import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;

// 저장/발행될 최종 이벤트 데이터
public record EnvironmentStatusEventDto(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String sensorType,
        ViolationType violationType,
        EnvironmentStatus previousStatus,
        EnvironmentStatus currentStatus,
        EnvironmentEventReason reason,
        Double value,
        Double min,
        Double max,
        String unit,
        String measuredAt,
        String message
) {
    public static EnvironmentStatusEventDto from(
            RuleResultDto ruleResultDto,
            EnvironmentEventDecisionDto environmentStatusChangeDto
    ){
        return new EnvironmentStatusEventDto(
                ruleResultDto.organizationId(),
                ruleResultDto.deviceEui(),
                ruleResultDto.storageId(),
                ruleResultDto.zoneId(),
                ruleResultDto.sensorType(),
                ruleResultDto.violationType(),
                environmentStatusChangeDto.previousStatus(),
                environmentStatusChangeDto.currentStatus(),
                environmentStatusChangeDto.reason(),
                ruleResultDto.value(),
                ruleResultDto.min(),
                ruleResultDto.max(),
                ruleResultDto.unit(),
                ruleResultDto.measuredAt(),
                ruleResultDto.message()
        );
    }
}
