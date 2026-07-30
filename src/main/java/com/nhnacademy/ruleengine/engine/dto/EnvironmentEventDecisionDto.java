package com.nhnacademy.ruleengine.engine.dto;

// 이벤트 생성을 위한 상태 전이 dto
public record EnvironmentEventDecisionDto(
        EnvironmentStatus previousStatus,
        EnvironmentStatus currentStatus,
        EnvironmentEventReason reason
) {
}
