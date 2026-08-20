package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.scheduler.ZoneDailySummaryRollupScheduler;
import com.nhnacademy.ruleengine.engine.service.ZoneDailySummaryArchiveService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine")
public class EnvironmentReportController {

    // 주간 리뷰가 보는 기본 기간.
    private static final int DEFAULT_PERIOD_DAYS = 7;

    private final ZoneDailySummaryArchiveService zoneDailySummaryArchiveService;
    private final ZoneDailySummaryRollupScheduler zoneDailySummaryRollupScheduler;

    /**
     * 구역의 하루치 환경 요약을 조회한다. date를 생략하면 어제를 대상으로 한다.
     * 적재된 요약이 있으면 그것을 쓰고, 없으면 원본에서 즉석 계산한다.
     */
    @GetMapping("/zones/{zoneId}/daily-summary")
    public ApiResponse<ZoneDailySummary> findDailySummary(
            @PathVariable Long zoneId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(
                zoneDailySummaryArchiveService.find(zoneId, yesterdayIfNull(date))
        );
    }

    /**
     * 구역의 하루 요약을 기간으로 조회한다.
     */
    @GetMapping("/zones/{zoneId}/daily-summaries")
    public ApiResponse<List<ZoneDailySummary>> findDailySummaries(
            @PathVariable Long zoneId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate end = yesterdayIfNull(to);
        LocalDate start = from != null
                ? from
                : end.minusDays(DEFAULT_PERIOD_DAYS - 1L);

        return ApiResponse.success(
                zoneDailySummaryArchiveService.findBetween(zoneId, start, end)
        );
    }

    /**
     * 하루 요약 적재를 수동으로 실행한다.
     */
    @PostMapping("/daily-summary-rollups")
    public ApiResponse<Void> rollup(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        zoneDailySummaryRollupScheduler.rollup(yesterdayIfNull(date));

        return ApiResponse.successNodata();
    }

    private LocalDate yesterdayIfNull(LocalDate date) {
        return date != null
                ? date
                : LocalDate.now(ZoneDailySummary.REPORT_ZONE).minusDays(1);
    }
}
