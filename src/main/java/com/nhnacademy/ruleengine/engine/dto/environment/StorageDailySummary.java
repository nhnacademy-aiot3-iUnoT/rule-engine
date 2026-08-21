package com.nhnacademy.ruleengine.engine.dto.environment;

import java.time.LocalDate;
import java.util.List;

/**
 * 저장소 하루치 환경 요약
 */
public record StorageDailySummary(
        Long storageId,
        LocalDate date,

        // 그날 데이터가 있었던 구역만 담긴다. 구역 순서는 구역 번호 순이다.
        List<ZoneDailySummary> zones
) {
    // 저장소 안 어느 구역에서도 수집된 데이터가 없으면 설명할 대상 자체가 없다.
    public boolean hasNoData() {
        return zones.isEmpty() || zones.stream().allMatch(ZoneDailySummary::hasNoData);
    }
}
