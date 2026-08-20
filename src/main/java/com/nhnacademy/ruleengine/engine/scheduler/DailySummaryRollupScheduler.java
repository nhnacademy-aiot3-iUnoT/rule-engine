package com.nhnacademy.ruleengine.engine.scheduler;

import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.service.StorageDailySummaryArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 매일 어제치 요약을 요약 버킷에 적재한다.
 * <p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailySummaryRollupScheduler {

    private final StorageDailySummaryArchiveService storageDailySummaryArchiveService;

    @Scheduled(cron = "${rule-engine.daily-rollup.cron}", zone = "Asia/Seoul")
    public void rollupYesterday() {
        LocalDate date = LocalDate.now(ZoneDailySummary.REPORT_ZONE).minusDays(1);

        try {
            storageDailySummaryArchiveService.rollup(date);

        } catch (RuntimeException exception) {
            // 스케줄러 밖으로 나간 예외는 로그 없이 사라지므로 여기서 반드시 남긴다.
            log.error("하루 요약 적재에 실패했습니다. date={}", date, exception);
        }
    }
}
