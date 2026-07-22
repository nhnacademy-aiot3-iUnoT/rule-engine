package com.nhnacademy.ruleengine.engine.dto;

// 환경 룰 필터 결과 dto
public record RuleResultDto(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String sensorType,
        String ruleType, //이상 판단 기준
        boolean violated,
        Double value,
        Double min,
        Double max,
        String unit,
        String measuredAt,
        Integer durationMinutes,
        String message
) {
}