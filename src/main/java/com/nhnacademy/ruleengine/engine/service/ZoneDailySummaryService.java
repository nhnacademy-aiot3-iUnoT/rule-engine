package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.DoorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyThresholdStat;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorDailyAggregate;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.repository.DailySummaryInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.SensorDailyStatRedisRepository;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 구역의 하루치 환경 요약생성
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneDailySummaryService {

    private static final String DOOR_SENSOR_TYPE = SensorType.DOOR.value();

    // door는 원본 이력으로 조회되어 집계 구간을 쓰지 않지만, 조회 메서드가 값을 요구한다.
    private static final String DOOR_WINDOW = "1h";

    private final SensorInfluxRepository sensorInfluxRepository;
    private final DailySummaryInfluxRepository dailySummaryInfluxRepository;
    private final SensorDailyStatRedisRepository sensorDailyStatRedisRepository;

    public ZoneDailySummary summarize(
            Long zoneId,
            LocalDate date
    ) {
        Instant from = startOfDay(date);
        Instant to = startOfDay(date.plusDays(1));

        List<SensorDailyAggregate> aggregates =
                sensorInfluxRepository.findDailyAggregatesByZone(zoneId, from, to);

        Map<String, Double> previousDayAvgs = findPreviousDayAvgs(zoneId, date);

        Map<String, SensorDailyThresholdStat> thresholdStats =
                sensorDailyStatRedisRepository.findByZoneAndDate(zoneId, date);

        List<SensorDailyStat> sensorStats = aggregates.stream()
                .map(aggregate -> toSensorStat(aggregate, thresholdStats, previousDayAvgs))
                .toList();

        return new ZoneDailySummary(
                zoneId,
                sensorStats,
                summarizeDoor(zoneId, from, to)
        );
    }

    /**
     * 전일 센서 타입별 평균을 찾는다.
     */
    private Map<String, Double> findPreviousDayAvgs(
            Long zoneId,
            LocalDate date
    ) {
        LocalDate previousDate = date.minusDays(1);

        Map<String, Double> stored = findStoredPreviousDayAvgs(zoneId, previousDate);

        if (!stored.isEmpty()) {
            return stored;
        }

        try {
            return sensorInfluxRepository.findDailyAggregatesByZone(
                            zoneId,
                            startOfDay(previousDate),
                            startOfDay(date)
                    ).stream()
                    .collect(Collectors.toMap(
                            SensorDailyAggregate::sensorType,
                            SensorDailyAggregate::avg,
                            (first, second) -> first
                    ));

        } catch (RuntimeException exception) {
            log.warn(
                    "전일 원본 조회에 실패해 비교 없이 요약합니다. zoneId={}, date={}",
                    zoneId,
                    previousDate
            );

            return Map.of();
        }
    }

    private Map<String, Double> findStoredPreviousDayAvgs(
            Long zoneId,
            LocalDate previousDate
    ) {
        try {
            return dailySummaryInfluxRepository
                    .findByZoneAndDate(zoneId, previousDate)
                    .map(ZoneDailySummary::sensorStats)
                    .orElseGet(List::of)
                    .stream()
                    .collect(Collectors.toMap(
                            SensorDailyStat::sensorType,
                            SensorDailyStat::avg,
                            (first, second) -> first
                    ));

        } catch (RuntimeException exception) {
            log.warn(
                    "저장된 전일 요약 조회에 실패해 원본으로 다시 시도합니다. zoneId={}, date={}",
                    zoneId,
                    previousDate
            );

            return Map.of();
        }
    }

    /**
     * 집계값에 그날의 임계값 판단 결과를 붙인다.
     * <p>
     * 통계가 없는 날은 임계값도 이탈 비율도 비운다. 지금 임계값을 끌어와 채우면
     * 그날 알림이 나갔는데도 이탈이 없었던 것처럼 보이는, 틀린 요약이 만들어진다.
     */
    private SensorDailyStat toSensorStat(
            SensorDailyAggregate aggregate,
            Map<String, SensorDailyThresholdStat> thresholdStats,
            Map<String, Double> previousDayAvgs
    ) {
        SensorDailyThresholdStat thresholdStat = thresholdStats.get(aggregate.sensorType());

        return new SensorDailyStat(
                aggregate.sensorType(),
                aggregate.unit(),
                aggregate.avg(),
                aggregate.min(),
                aggregate.max(),
                thresholdStat == null ? null : thresholdStat.min(),
                thresholdStat == null ? null : thresholdStat.max(),
                thresholdStat == null ? null : thresholdStat.outOfRangeRatio(),
                previousDayAvgs.get(aggregate.sensorType())
        );
    }

    /**
     * 문 개폐를 열린 횟수와 누적 열림 시간으로 집계한다.
     */
    private DoorDailyStat summarizeDoor(
            Long zoneId,
            Instant from,
            Instant to
    ) {
        List<SensorHistoryResponse> history = sensorInfluxRepository.findHistoryByZone(
                zoneId,
                DOOR_SENSOR_TYPE,
                from,
                to,
                DOOR_WINDOW
        );

        if (history.isEmpty()) {
            return null;
        }

        List<SensorHistoryResponse> sorted = new ArrayList<>(history);
        sorted.sort(Comparator.comparing(SensorHistoryResponse::time));

        long openCount = 0L;
        long openSeconds = 0L;
        boolean previouslyOpen = false;

        for (int index = 0; index < sorted.size(); index++) {
            SensorHistoryResponse response = sorted.get(index);
            boolean open = isOpen(response);

            if (open && !previouslyOpen) {
                openCount++;
            }

            if (open) {
                Instant until = index + 1 < sorted.size()
                        ? sorted.get(index + 1).time()
                        : to;

                openSeconds += Duration.between(response.time(), until).toSeconds();
            }

            previouslyOpen = open;
        }

        return new DoorDailyStat(
                openCount,
                Duration.ofSeconds(openSeconds).toMinutes()
        );
    }

    private boolean isOpen(SensorHistoryResponse response) {
        return response.value() != null && response.value() > 0.0;
    }

    private Instant startOfDay(LocalDate date) {
        return ZoneDailySummary.startOfDay(date);
    }
}
