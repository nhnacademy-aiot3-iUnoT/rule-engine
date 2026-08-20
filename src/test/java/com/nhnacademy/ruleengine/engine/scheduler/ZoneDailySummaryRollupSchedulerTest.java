package com.nhnacademy.ruleengine.engine.scheduler;

import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.service.RedisLeaseLockService;
import com.nhnacademy.ruleengine.engine.service.ZoneDailySummaryArchiveService;
import com.nhnacademy.ruleengine.global.config.DailyRollupProperties;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties.LeaseSettings;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneDailySummaryRollupSchedulerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 17);
    private static final String LOCK_KEY = "test:lock:daily-rollup:2026-08-17";

    @Mock
    private ZoneDailySummaryArchiveService zoneDailySummaryArchiveService;

    @Mock
    private RedisLeaseLockService redisLeaseLockService;

    private ZoneDailySummaryRollupScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ZoneDailySummaryRollupScheduler(
                zoneDailySummaryArchiveService,
                redisLeaseLockService,
                new DailyRollupProperties(
                        "0 10 0 * * *",
                        "test:lock:daily-rollup",
                        Duration.ofHours(6)
                ),
                new RedundancyProperties(
                        "rule-engine-1",
                        leaseSettings(),
                        leaseSettings()
                )
        );
    }

    @Test
    @DisplayName("Lock을 잡은 인스턴스만 적재한다")
    void rollupsOnlyWithLock() {
        when(redisLeaseLockService.acquire(eq(LOCK_KEY), anyString(), any()))
                .thenReturn(false);

        scheduler.rollup(DATE);

        verify(zoneDailySummaryArchiveService, never()).rollup(any());
    }

    @Test
    @DisplayName("적재에 성공하면 Lock을 유지해 같은 날짜를 다시 적재하지 않는다")
    void keepsLockAfterSuccess() {
        when(redisLeaseLockService.acquire(eq(LOCK_KEY), anyString(), any()))
                .thenReturn(true);

        scheduler.rollup(DATE);

        verify(zoneDailySummaryArchiveService).rollup(DATE);
        verify(redisLeaseLockService, never()).release(anyString(), anyString());
    }

    @Test
    @DisplayName("적재에 실패하면 Lock을 반납해 다시 시도할 수 있게 한다")
    void releasesLockAfterFailure() {
        when(redisLeaseLockService.acquire(eq(LOCK_KEY), anyString(), any()))
                .thenReturn(true);

        when(zoneDailySummaryArchiveService.rollup(DATE))
                .thenThrow(new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED));

        scheduler.rollup(DATE);

        verify(redisLeaseLockService).release(eq(LOCK_KEY), anyString());
    }

    private LeaseSettings leaseSettings() {
        return new LeaseSettings(
                "test:lock",
                Duration.ofSeconds(10),
                Duration.ofSeconds(3)
        );
    }
}
