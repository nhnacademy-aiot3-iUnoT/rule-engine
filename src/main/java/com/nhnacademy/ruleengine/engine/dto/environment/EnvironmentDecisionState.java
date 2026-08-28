package com.nhnacademy.ruleengine.engine.dto.environment;

import java.time.Instant;

// 센서별 환경상태 판단 상태. 서버 인스턴스 간 공유를 위해 Redis에 저장된다.
public record EnvironmentDecisionState(
        EnvStatus state,
        Instant firstViolatedAt,
        Instant lastAlertAt,
        Instant firstNormalAt,
        Instant lastMeasuredAt
) {
}
