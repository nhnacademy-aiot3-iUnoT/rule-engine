package com.nhnacademy.ruleengine.engine.dto.environment;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

// 구역 하루치 환경 요약. AI 설명의 입력이 되지만 AI 없이도 그 자체로 쓸 수 있는 결과물이다.
//
// 평균·이탈 비율 같은 수치는 전부 여기서 확정된다. AI에게 계산을 맡기면 그럴듯한 오답이 나오므로
// 숫자는 InfluxDB와 이 계층에서 끝내고, AI는 확정된 숫자를 문장으로 옮기는 역할만 맡는다.
public record ZoneDailySummary(
        Long zoneId,
        LocalDate date,
        List<SensorDailyStat> sensorStats,

        // 문 센서가 없거나 하루 동안 기록이 없으면 null이다.
        // 0회로 채우면 "문이 안 열렸다"와 "문 센서가 없다"가 구분되지 않는다.
        DoorDailyStat door
) {
    // 하루의 경계는 보는 사람의 시간대를 따라야 "어제 하루"가 상식과 맞는다.
    // 계산할 때와 저장·조회할 때의 경계가 어긋나면 하루가 통째로 밀리므로 기준을 한 곳에 둔다.
    public static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Seoul");

    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(REPORT_ZONE).toInstant();
    }

    public static LocalDate dateOf(Instant instant) {
        return instant.atZone(REPORT_ZONE).toLocalDate();
    }

    // 하루 동안 수집된 데이터가 없으면 설명할 대상 자체가 없다.
    public boolean hasNoData() {
        return sensorStats.isEmpty() && door == null;
    }
}
