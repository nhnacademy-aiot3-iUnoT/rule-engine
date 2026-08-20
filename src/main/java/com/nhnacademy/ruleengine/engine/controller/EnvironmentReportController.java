package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.environment.DailySummaryRollupResult;
import com.nhnacademy.ruleengine.engine.dto.environment.StorageDailySummary;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.service.StorageDailySummaryArchiveService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine")
public class EnvironmentReportController {

    // 주간 리뷰가 보는 기본 기간.
    private static final int DEFAULT_PERIOD_DAYS = 7;

    private final StorageDailySummaryArchiveService storageDailySummaryArchiveService;


    /**
     * 저장소의 하루 요약을 기간으로 조회한다. 기본은 어제까지의 최근 7일이다.
     */
    @GetMapping("/storages/{storageId}/daily-summaries")
    public ApiResponse<List<StorageDailySummary>> findDailySummaries(
            @PathVariable Long storageId,
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
                storageDailySummaryArchiveService.findBetween(storageId, start, end)
        );
    }

    /**
     * 하루 요약 적재를 수동으로 실행한다. date를 생략하면 어제를 대상으로 한다.(테스트 용도)
     */
    @PostMapping("/daily-summary-rollups")
    public ApiResponse<DailySummaryRollupResult> rollup(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate target = yesterdayIfNull(date);

        return ApiResponse.success(
                new DailySummaryRollupResult(
                        target,
                        storageDailySummaryArchiveService.rollup(target)
                )
        );
    }

    /**
     * 날짜가 없는경우 어제로 설정
     */
    private LocalDate yesterdayIfNull(LocalDate date) {
        return date != null
                ? date
                : LocalDate.now(ZoneDailySummary.REPORT_ZONE).minusDays(1);
    }
}
