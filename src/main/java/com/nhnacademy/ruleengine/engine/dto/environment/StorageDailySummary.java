package com.nhnacademy.ruleengine.engine.dto.environment;

import java.time.LocalDate;
import java.util.List;

/**
 * 저장소 하루치 환경 요약.
 * <p>
 * 사람이 창고를 볼 때의 단위는 구역이 아니라 저장소다. "3번 창고 어제 어땠나"를 물었을 때
 * 구역마다 따로 조회해 합치게 하면 화면과 리포트가 그 조립을 매번 다시 하게 된다.
 * <p>
 * 저장소 전체를 가로지르는 평균 같은 값은 두지 않는다. 구역마다 임계값이 다르고 센서 구성도 달라
 * 섞어 놓은 평균은 어떤 판단의 근거도 되지 못한다. 판단은 구역 단위로 남는다.
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
