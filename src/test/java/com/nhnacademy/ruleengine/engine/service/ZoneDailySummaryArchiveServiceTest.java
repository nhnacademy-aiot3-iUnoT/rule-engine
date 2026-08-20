package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.ZoneDailySummaryInfluxRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneDailySummaryArchiveServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);

    @Mock
    private ZoneDailySummaryService zoneDailySummaryService;

    @Mock
    private ZoneDailySummaryInfluxRepository zoneDailySummaryInfluxRepository;

    @Mock
    private SensorInfluxRepository sensorInfluxRepository;

    @InjectMocks
    private ZoneDailySummaryArchiveService zoneDailySummaryArchiveService;

    @Test
    @DisplayName("데이터가 들어온 모든 구역의 요약을 적재한다")
    void rollupsEveryZone() {
        when(sensorInfluxRepository.findZoneIds(
                ZoneDailySummary.startOfDay(DATE),
                ZoneDailySummary.startOfDay(DATE.plusDays(1))
        )).thenReturn(List.of(1L, 2L));

        when(zoneDailySummaryService.summarize(anyLong(), any()))
                .thenAnswer(invocation -> summary(invocation.getArgument(0)));

        int savedCount = zoneDailySummaryArchiveService.rollup(DATE);

        assertAll(
                () -> assertEquals(2, savedCount),
                () -> verify(zoneDailySummaryInfluxRepository).save(summary(1L)),
                () -> verify(zoneDailySummaryInfluxRepository).save(summary(2L))
        );
    }

    @Test
    @DisplayName("한 구역이 실패해도 나머지 구역은 적재한다")
    void keepsGoingAfterZoneFailure() {
        when(sensorInfluxRepository.findZoneIds(any(), any()))
                .thenReturn(List.of(1L, 2L));

        when(zoneDailySummaryService.summarize(1L, DATE))
                .thenThrow(new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED));

        when(zoneDailySummaryService.summarize(2L, DATE))
                .thenReturn(summary(2L));

        int savedCount = zoneDailySummaryArchiveService.rollup(DATE);

        assertAll(
                () -> assertEquals(1, savedCount),
                () -> verify(zoneDailySummaryInfluxRepository).save(summary(2L))
        );
    }

    @Test
    @DisplayName("저장된 요약이 있으면 다시 계산하지 않는다")
    void prefersStoredSummary() {
        when(zoneDailySummaryInfluxRepository.findByZoneAndDate(1L, DATE))
                .thenReturn(Optional.of(summary(1L)));

        ZoneDailySummary found = zoneDailySummaryArchiveService.find(1L, DATE);

        assertAll(
                () -> assertEquals(summary(1L), found),
                () -> verify(zoneDailySummaryService, never()).summarize(anyLong(), any())
        );
    }

    @Test
    @DisplayName("저장된 요약이 없으면 원본에서 계산한다")
    void computesWhenNotStored() {
        when(zoneDailySummaryInfluxRepository.findByZoneAndDate(1L, DATE))
                .thenReturn(Optional.empty());

        when(zoneDailySummaryService.summarize(1L, DATE))
                .thenReturn(summary(1L));

        assertEquals(summary(1L), zoneDailySummaryArchiveService.find(1L, DATE));
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 조회하지 않는다")
    void rejectsInvertedPeriod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> zoneDailySummaryArchiveService.findBetween(1L, DATE, DATE.minusDays(1))
        );
    }

    private ZoneDailySummary summary(Long zoneId) {
        return new ZoneDailySummary(
                zoneId,
                DATE,
                List.of(new SensorDailyStat(
                        "temperature", "C", 24.0, 20.0, 28.0,
                        null, null, null, null
                )),
                null
        );
    }
}
