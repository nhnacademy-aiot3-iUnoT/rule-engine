package com.nhnacademy.ruleengine.engine.dto.environment;

import java.time.LocalDate;

/**
 * 하루 요약 적재의 결과.
 * <p>
 * 응답이 성공이라는 사실만으로는 몇 개 구역이 실제로 적재됐는지 알 수 없어 함께 돌려준다.
 * 대상 구역이 없던 날과 일부 구역이 실패한 날을 호출한 쪽에서 구분할 수 있어야 한다.
 */
public record ZoneDailySummaryRollupResult(
        LocalDate date,
        int savedZoneCount
) {
}
