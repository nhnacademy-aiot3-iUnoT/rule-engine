package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyThresholdStat;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorDailyAggregate;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.repository.SensorDailyStatRedisRepository;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.ZoneDailySummaryInfluxRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneDailySummaryServiceTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final Long ZONE_ID = 3L;

    private static final Instant DAY_START = DATE.atStartOfDay(KST).toInstant();
    private static final Instant DAY_END = DATE.plusDays(1).atStartOfDay(KST).toInstant();

    @Mock
    private SensorInfluxRepository sensorInfluxRepository;

    @Mock
    private ZoneDailySummaryInfluxRepository zoneDailySummaryInfluxRepository;

    @Mock
    private SensorDailyStatRedisRepository sensorDailyStatRedisRepository;

    @InjectMocks
    private ZoneDailySummaryService zoneDailySummaryService;

    @BeforeEach
    void stubDefaults() {
        lenient().when(sensorInfluxRepository.findHistoryByZone(
                anyLong(), anyString(), any(), any(), anyString()
        )).thenReturn(List.of());

        lenient().when(sensorDailyStatRedisRepository.findByZoneAndDate(anyLong(), any()))
                .thenReturn(Map.of());

        lenient().when(zoneDailySummaryInfluxRepository.findByZoneAndDate(anyLong(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("하루 경계를 KST 자정으로 잡아 조회한다")
    void queriesWithKoreanDayBoundary() {
        stubAggregates(List.of(), List.of());

        zoneDailySummaryService.summarize(ZONE_ID, DATE);

        verify(sensorInfluxRepository).findDailyAggregatesByZone(ZONE_ID, DAY_START, DAY_END);
    }

    @Test
    @DisplayName("판단 시점에 기록해 둔 임계값과 이탈 비율, 전일 평균을 붙여 요약한다")
    void summarizesWithRecordedThreshold() {
        stubAggregates(
                List.of(new SensorDailyAggregate("temperature", "C", 100L, 24.5, 18.0, 31.0)),
                List.of(new SensorDailyAggregate("temperature", "C", 90L, 22.5, 19.0, 26.0))
        );

        when(sensorDailyStatRedisRepository.findByZoneAndDate(ZONE_ID, DATE))
                .thenReturn(Map.of(
                        "temperature",
                        new SensorDailyThresholdStat("temperature", 100L, 12L, 10.0, 30.0)
                ));

        SensorDailyStat stat = zoneDailySummaryService
                .summarize(ZONE_ID, DATE)
                .sensorStats()
                .getFirst();

        assertAll(
                () -> assertEquals(24.5, stat.avg()),
                () -> assertEquals(10.0, stat.thresholdMin()),
                () -> assertEquals(30.0, stat.thresholdMax()),
                () -> assertEquals(0.12, stat.outOfRangeRatio()),
                () -> assertEquals(22.5, stat.previousDayAvg())
        );
    }

    @Test
    @DisplayName("그날의 임계값 기록이 없으면 임계값도 이탈 비율도 비운다")
    void leavesThresholdEmptyWithoutRecordedStat() {
        stubAggregates(
                List.of(new SensorDailyAggregate("humidity", "%", 100L, 55.0, 40.0, 70.0)),
                List.of()
        );

        SensorDailyStat stat = zoneDailySummaryService
                .summarize(ZONE_ID, DATE)
                .sensorStats()
                .getFirst();

        assertAll(
                () -> assertNull(stat.thresholdMin()),
                () -> assertNull(stat.thresholdMax()),
                () -> assertNull(stat.outOfRangeRatio()),
                () -> assertNull(stat.previousDayAvg())
        );
    }

    @Test
    @DisplayName("저장된 전일 요약이 있으면 원본 대신 그 평균을 쓴다")
    void prefersStoredPreviousDaySummary() {
        stubAggregates(
                List.of(new SensorDailyAggregate("temperature", "C", 100L, 24.5, 18.0, 31.0)),
                List.of()
        );

        when(zoneDailySummaryInfluxRepository.findByZoneAndDate(ZONE_ID, DATE.minusDays(1)))
                .thenReturn(Optional.of(new ZoneDailySummary(
                        ZONE_ID,
                        DATE.minusDays(1),
                        List.of(new SensorDailyStat(
                                "temperature", "C", 21.0, 18.0, 25.0,
                                null, null, null, null
                        )),
                        null
                )));

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertAll(
                () -> assertEquals(21.0, summary.sensorStats().getFirst().previousDayAvg()),
                () -> verify(sensorInfluxRepository, never()).findDailyAggregatesByZone(
                        ZONE_ID,
                        DATE.minusDays(1).atStartOfDay(KST).toInstant(),
                        DAY_START
                )
        );
    }

    @Test
    @DisplayName("저장된 전일 요약 조회가 실패해도 원본으로 되돌아간다")
    void fallsBackToRawWhenStoredLookupFails() {
        stubAggregates(
                List.of(new SensorDailyAggregate("temperature", "C", 100L, 24.5, 18.0, 31.0)),
                List.of(new SensorDailyAggregate("temperature", "C", 90L, 22.5, 19.0, 26.0))
        );

        when(zoneDailySummaryInfluxRepository.findByZoneAndDate(ZONE_ID, DATE.minusDays(1)))
                .thenThrow(new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED));

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertEquals(22.5, summary.sensorStats().getFirst().previousDayAvg());
    }

    @Test
    @DisplayName("전일 조회가 모두 실패해도 오늘 요약은 만들어진다")
    void survivesPreviousDayFailure() {
        when(sensorInfluxRepository.findDailyAggregatesByZone(ZONE_ID, DAY_START, DAY_END))
                .thenReturn(List.of(new SensorDailyAggregate("temperature", "C", 10L, 24.0, 20.0, 28.0)));

        when(sensorInfluxRepository.findDailyAggregatesByZone(
                eq(ZONE_ID), eq(DATE.minusDays(1).atStartOfDay(KST).toInstant()), eq(DAY_START)
        )).thenThrow(new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED));

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertNull(summary.sensorStats().getFirst().previousDayAvg());
    }

    @Test
    @DisplayName("문은 열린 횟수와 누적 열림 시간으로 집계한다")
    void summarizesDoor() {
        stubAggregates(List.of(), List.of());

        when(sensorInfluxRepository.findHistoryByZone(
                eq(ZONE_ID), eq("door"), eq(DAY_START), eq(DAY_END), anyString()
        )).thenReturn(List.of(
                doorRecord(DAY_START.plusSeconds(3600), 1.0),          // 열림
                doorRecord(DAY_START.plusSeconds(5400), 0.0),          // 30분 뒤 닫힘
                doorRecord(DAY_START.plusSeconds(7200), 1.0),          // 다시 열림
                doorRecord(DAY_START.plusSeconds(8400), 0.0)           // 20분 뒤 닫힘
        ));

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertAll(
                () -> assertEquals(2L, summary.door().openCount()),
                () -> assertEquals(50L, summary.door().openMinutes())
        );
    }

    @Test
    @DisplayName("닫히지 않은 문은 하루가 끝날 때까지 열려 있던 것으로 센다")
    void countsDoorOpenUntilEndOfDay() {
        stubAggregates(List.of(), List.of());

        when(sensorInfluxRepository.findHistoryByZone(
                eq(ZONE_ID), eq("door"), eq(DAY_START), eq(DAY_END), anyString()
        )).thenReturn(List.of(doorRecord(DAY_END.minusSeconds(600), 1.0)));

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertEquals(10L, summary.door().openMinutes());
    }

    @Test
    @DisplayName("문 기록이 없으면 0회가 아니라 비워 둔다")
    void leavesDoorNullWithoutRecords() {
        stubAggregates(
                List.of(new SensorDailyAggregate("temperature", "C", 10L, 24.0, 20.0, 28.0)),
                List.of()
        );

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertNull(summary.door());
    }

    @Test
    @DisplayName("수집된 데이터가 없으면 빈 요약임을 알린다")
    void reportsNoData() {
        stubAggregates(List.of(), List.of());

        ZoneDailySummary summary =
                zoneDailySummaryService.summarize(ZONE_ID, DATE);

        assertTrue(summary.hasNoData());
    }

    private void stubAggregates(
            List<SensorDailyAggregate> today,
            List<SensorDailyAggregate> previousDay
    ) {
        when(sensorInfluxRepository.findDailyAggregatesByZone(ZONE_ID, DAY_START, DAY_END))
                .thenReturn(today);

        lenient().when(sensorInfluxRepository.findDailyAggregatesByZone(
                ZONE_ID, DATE.minusDays(1).atStartOfDay(KST).toInstant(), DAY_START
        )).thenReturn(previousDay);
    }

    private SensorHistoryResponse doorRecord(Instant time, Double value) {
        return new SensorHistoryResponse("door", "문열림 여부", time, value);
    }
}
