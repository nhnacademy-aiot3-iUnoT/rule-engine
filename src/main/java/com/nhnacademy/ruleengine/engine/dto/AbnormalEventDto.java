package com.nhnacademy.ruleengine.engine.dto;

import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;

public record AbnormalEventDto(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String sensorType,
        ViolationType violationType,
        EnvironmentStatus previousStatus,
        EnvironmentStatus currentStatus,
        Double value,
        Double min,
        Double max,
        String unit,
        String measuredAt,
        String message
) {
    public static AbnormalEventDto from(
            RuleResultDto ruleResultDto,
            EnvironmentEventDecisionDto environmentStatusChangeDto
    ){
        return new AbnormalEventDto(
                ruleResultDto.organizationId(),
                ruleResultDto.deviceEui(),
                ruleResultDto.storageId(),
                ruleResultDto.sectionId(),
                ruleResultDto.sensorType(),
                ruleResultDto.violationType(),
                environmentStatusChangeDto.previousStatus(),
                environmentStatusChangeDto.currentStatus(),
                ruleResultDto.value(),
                ruleResultDto.min(),
                ruleResultDto.max(),
                ruleResultDto.unit(),
                ruleResultDto.measuredAt(),
                ruleResultDto.message()
        );
    }
}
