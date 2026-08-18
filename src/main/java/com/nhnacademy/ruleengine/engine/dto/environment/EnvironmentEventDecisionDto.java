package com.nhnacademy.ruleengine.engine.dto.environment;

// 이벤트 생성을 위한 상태 전이 dto
public record EnvironmentEventDecisionDto(
        EnvStatus previousStatus,
        EnvStatus currentStatus,
        EnvironmentEventReason reason
) {
}
