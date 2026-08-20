package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.DoorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorDailyAggregate;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.ZoneDailySummaryInfluxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    private final ZoneDailySummaryInfluxRepository zoneDailySummaryInfluxRepository;
    private final ThresholdPolicyService thresholdPolicyService;

    public ZoneDailySummary summarizeYesterday(Long zoneId) {
        return summarize(zoneId, LocalDate.now(ZoneDailySummary.REPORT_ZONE).minusDays(1));
    }

    public ZoneDailySummary summarize(
            Long zoneId,
            LocalDate date
    ) {
        Instant from = startOfDay(date);
        Instant to = startOfDay(date.plusDays(1));

        List<SensorDailyAggregate> aggregates =
                sensorInfluxRepository.findDailyAggregatesByZone(zoneId, from, to);

        Map<String, Double> previousDayAvgs = findPreviousDayAvgs(zoneId, date);

        ThresholdPolicyDto policy = thresholdPolicyService.getThresholdPolicy(zoneId);

        List<SensorDailyStat> sensorStats = aggregates.stream()
                .map(aggregate -> toSensorStat(zoneId, aggregate, policy, previousDayAvgs, from, to))
                .toList();

        return new ZoneDailySummary(
                zoneId,
                date,
                sensorStats,
                summarizeDoor(zoneId, from, to)
        );
    }

    /**
     * 전일 센서 타입별 평균을 찾는다.
     * <p>
     * 저장된 요약을 먼저 본다. 원본 버킷의 보관 기간이 이틀이라, 어제치를 요약하는 시점에
     * 그제 원본은 이미 지워져 있다. 요약 버킷에는 남아 있으므로 비교가 끊기지 않는다.
     * 아직 적재되지 않은 날짜(오늘 요약을 즉석에서 만드는 경우)는 원본으로 되돌아가 계산한다.
     * <p>
     * 전일 평균은 비교용 부가 정보라, 어느 쪽 조회가 실패하더라도 오늘 요약까지 함께 실패시키지 않는다.
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
                    previousDate,
                    exception
            );

            return Map.of();
        }
    }

    private Map<String, Double> findStoredPreviousDayAvgs(
            Long zoneId,
            LocalDate previousDate
    ) {
        try {
            return zoneDailySummaryInfluxRepository
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
                    previousDate,
                    exception
            );

            return Map.of();
        }
    }

    private SensorDailyStat toSensorStat(
            Long zoneId,
            SensorDailyAggregate aggregate,
            ThresholdPolicyDto policy,
            Map<String, Double> previousDayAvgs,
            Instant from,
            Instant to
    ) {
        Optional<ThresholdRange> range = policy.rangeFor(aggregate.sensorType());

        Double thresholdMin = range.map(ThresholdRange::min).orElse(null);
        Double thresholdMax = range.map(ThresholdRange::max).orElse(null);

        return new SensorDailyStat(
                aggregate.sensorType(),
                aggregate.unit(),
                aggregate.avg(),
                aggregate.min(),
                aggregate.max(),
                thresholdMin,
                thresholdMax,
                calculateOutOfRangeRatio(zoneId, aggregate, thresholdMin, thresholdMax, from, to),
                previousDayAvgs.get(aggregate.sensorType())
        );
    }

    // 임계값이 없으면 "벗어났다"는 판단 자체가 성립하지 않으므로 0이 아니라 null이다.
    private Double calculateOutOfRangeRatio(
            Long zoneId,
            SensorDailyAggregate aggregate,
            Double thresholdMin,
            Double thresholdMax,
            Instant from,
            Instant to
    ) {
        if (aggregate.count() == 0L || (thresholdMin == null && thresholdMax == null)) {
            return null;
        }

        long outOfRangeCount = sensorInfluxRepository.countOutOfRange(
                zoneId,
                aggregate.sensorType(),
                thresholdMin,
                thresholdMax,
                from,
                to
        );

        return round((double) outOfRangeCount / aggregate.count(), 3);
    }

    /**
     * 문 개폐를 열린 횟수와 누적 열림 시간으로 집계한다.
     * <p>
     * door는 상태가 바뀔 때만 기록되어 하루 표본이 적으므로 원본을 받아 계산한다.
     * 한 기록의 상태는 다음 기록 시각까지 유지되고, 마지막 기록의 상태는 하루가 끝날 때까지 이어진다.
     * 기록이 아예 없으면 문 센서가 없는 구역과 구분되지 않으므로 null을 돌려준다.
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
            SensorHistoryResponse record = sorted.get(index);
            boolean open = isOpen(record);

            if (open && !previouslyOpen) {
                openCount++;
            }

            if (open) {
                Instant until = index + 1 < sorted.size()
                        ? sorted.get(index + 1).time()
                        : to;

                openSeconds += Duration.between(record.time(), until).toSeconds();
            }

            previouslyOpen = open;
        }

        return new DoorDailyStat(
                openCount,
                Duration.ofSeconds(openSeconds).toMinutes()
        );
    }

    private boolean isOpen(SensorHistoryResponse record) {
        return record.value() != null && record.value() > 0.0;
    }

    private Instant startOfDay(LocalDate date) {
        return ZoneDailySummary.startOfDay(date);
    }

    private double round(double value, int scale) {
        return BigDecimal.valueOf(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
