package com.nhnacademy.ruleengine.engine.dto;

import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;

// 환경 룰 필터 결과 dto
public record RuleResultDto(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String sensorType,
        ViolationType violationType, //이상 타입
        boolean violated,
        Double value,
        Double min,
        Double max,
        String unit,
        String measuredAt,
        Integer durationMinutes,
        String message
) {
    public static RuleResultDto fromThreshold(RuleResultCreateRequest request){
        return new RuleResultDto(
                request.sensorPayload().organizationId(),
                request.sensorPayload().deviceEui(),
                request.sensorPayload().storageId(),
                request.sensorPayload().sectionId(),
                request.sensorPayload().sensorType(),
                request.violationType(),
                request.violated(),
                request.sensorPayload().value(),
                request.min(),
                request.max(),
                request.sensorPayload().unit(),
                request.sensorPayload().time(),
                request.thresholdDurationMinutes(),
                request.message()
                );
    }

    public static RuleResultDto fromDoorState(RuleResultCreateRequest request){
        return new RuleResultDto(
                request.sensorPayload().organizationId(),
                request.sensorPayload().deviceEui(),
                request.sensorPayload().storageId(),
                request.sensorPayload().sectionId(),
                request.sensorPayload().sensorType(),
                request.violationType(),
                request.violated(),
                request.sensorPayload().value(),
                null,
                null,
                request.sensorPayload().unit(),

                request.sensorPayload().time(),
                null,
                request.message()
        );
    }
}