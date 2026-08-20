package com.nhnacademy.ruleengine.engine.scheduler;

import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.service.RedisLeaseLockService;
import com.nhnacademy.ruleengine.engine.service.ZoneDailySummaryArchiveService;
import com.nhnacademy.ruleengine.global.config.DailyRollupProperties;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 매일 어제치 요약을 요약 버킷에 적재한다.
 */
@Slf4j
@Component
public class ZoneDailySummaryRollupScheduler {

    private final ZoneDailySummaryArchiveService zoneDailySummaryArchiveService;
    private final RedisLeaseLockService redisLeaseLockService;
    private final DailyRollupProperties dailyRollupProperties;
    private final String ownerToken;

    public ZoneDailySummaryRollupScheduler(
            ZoneDailySummaryArchiveService zoneDailySummaryArchiveService,
            RedisLeaseLockService redisLeaseLockService,
            DailyRollupProperties dailyRollupProperties,
            RedundancyProperties redundancyProperties
    ) {
        this.zoneDailySummaryArchiveService = zoneDailySummaryArchiveService;
        this.redisLeaseLockService = redisLeaseLockService;
        this.dailyRollupProperties = dailyRollupProperties;
        this.ownerToken = redundancyProperties.instanceId() + ":" + UUID.randomUUID();
    }

    @Scheduled(cron = "${rule-engine.daily-rollup.cron}", zone = "Asia/Seoul")
    public void rollupYesterday() {
        rollup(LocalDate.now(ZoneDailySummary.REPORT_ZONE).minusDays(1));
    }

    /**
     * 날짜별 Lock을 잡은 인스턴스만 적재한다.
     */
    public void rollup(LocalDate date) {
        String lockKey = dailyRollupProperties.lockKeyPrefix() + ":" + date;

        if (!redisLeaseLockService.acquire(
                lockKey,
                ownerToken,
                dailyRollupProperties.lockDuration()
        )) {
            log.info(
                    "다른 인스턴스가 이미 적재했거나 적재 중입니다. date={}, currentOwner={}",
                    date,
                    redisLeaseLockService.getOwner(lockKey)
            );

            return;
        }

        try {
            zoneDailySummaryArchiveService.rollup(date);

        } catch (RuntimeException exception) {
            log.error("하루 요약 적재에 실패했습니다. date={}", date, exception);

            redisLeaseLockService.release(lockKey, ownerToken);
        }
    }
}
