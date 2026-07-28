package com.nhnacademy.ruleengine.engine.dto;

// 환경 임계값 설정 dto
public record ThresholdPolicyDto(
        Long organizationId,
        Long locationId,
        Long positionId,
        Double minTemperature,
        Double maxTemperature,
        Double minHumidity,
        Double maxHumidity,
        Integer thresholdDurationMinutes

) {
}
