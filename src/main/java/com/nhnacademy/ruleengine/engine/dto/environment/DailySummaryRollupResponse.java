package com.nhnacademy.ruleengine.engine.dto.environment;

import java.time.LocalDate;

/**
 * 하루 요약 적재의 결과.
 */
public record DailySummaryRollupResponse(
        LocalDate date,
        int savedZoneCount
) {
}
