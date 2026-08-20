package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.StorageDailySummary;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.repository.DailySummaryInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StorageDailySummaryArchiveServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final Long STORAGE_ID = 1L;

    @Mock
    private ZoneDailySummaryService zoneDailySummaryService;

    @Mock
    private DailySummaryInfluxRepository dailySummaryInfluxRepository;

    @Mock
    private SensorInfluxRepository sensorInfluxRepository;

    @InjectMocks
    private StorageDailySummaryArchiveService storageDailySummaryArchiveService;

    @Test
    @DisplayName("저장소마다 그 안의 구역 요약을 모두 적재한다")
    void rollupsEveryZoneOfEveryStorage() {
        when(sensorInfluxRepository.findStorageIds(any(), any()))
                .thenReturn(List.of(1L, 2L));

        when(sensorInfluxRepository.findZoneIdsByStorage(anyLong(), any(), any()))
                .thenReturn(List.of(21L, 22L));

        when(zoneDailySummaryService.summarize(anyLong(), any()))
                .thenAnswer(invocation -> zone(invocation.getArgument(0)));

        int savedCount = storageDailySummaryArchiveService.rollup(DATE);

        assertAll(
                () -> assertEquals(4, savedCount),
                () -> verify(dailySummaryInfluxRepository).save(
                        new StorageDailySummary(1L, DATE, List.of(zone(21L), zone(22L)))),
                () -> verify(dailySummaryInfluxRepository).save(
                        new StorageDailySummary(2L, DATE, List.of(zone(21L), zone(22L))))
        );
    }

    @Test
    @DisplayName("한 구역이 실패해도 나머지 구역은 적재한다")
    void keepsGoingAfterZoneFailure() {
        when(sensorInfluxRepository.findStorageIds(any(), any())).thenReturn(List.of(1L));
        when(sensorInfluxRepository.findZoneIdsByStorage(anyLong(), any(), any()))
                .thenReturn(List.of(21L, 22L));

        when(zoneDailySummaryService.summarize(21L, DATE))
                .thenThrow(new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED));

        when(zoneDailySummaryService.summarize(22L, DATE))
                .thenReturn(zone(22L));

        assertAll(
                () -> assertEquals(1, storageDailySummaryArchiveService.rollup(DATE)),
                () -> verify(dailySummaryInfluxRepository).save(
                        new StorageDailySummary(STORAGE_ID, DATE, List.of(zone(22L))))
        );
    }

    @Test
    @DisplayName("데이터가 들어온 저장소가 없으면 아무것도 적재하지 않는다")
    void skipsWithoutStorages() {
        when(sensorInfluxRepository.findStorageIds(any(), any())).thenReturn(List.of());

        assertAll(
                () -> assertEquals(0, storageDailySummaryArchiveService.rollup(DATE)),
                () -> verify(dailySummaryInfluxRepository, never()).save(any())
        );
    }

    @Test
    @DisplayName("시작일이 종료일보다 늦으면 조회하지 않는다")
    void rejectsInvertedPeriod() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageDailySummaryArchiveService.findBetween(STORAGE_ID, DATE, DATE.minusDays(1))
        );
    }

    private ZoneDailySummary zone(Long zoneId) {
        return new ZoneDailySummary(
                zoneId,
                List.of(new SensorDailyStat(
                        "temperature", "C", 24.0, 20.0, 28.0,
                        null, null, null, null
                )),
                null
        );
    }
}
