package com.nhnacademy.ruleengine.engine.dto.environment;

// 문 개폐의 하루 통계.
public record DoorDailyStat(
        long openCount,
        long openMinutes
) {
}
