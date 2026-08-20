package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.ZoneDailySummaryInfluxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 하루 요약을 요약 버킷에 팅적재하고 다시 꺼내 온다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneDailySummaryArchiveService {

    private final ZoneDailySummaryService zoneDailySummaryService;
    private final ZoneDailySummaryInfluxRepository zoneDailySummaryInfluxRepository;
    private final SensorInfluxRepository sensorInfluxRepository;

    /**
     * 해당 날짜에 데이터가 들어온 모든 구역의 요약을 적재하고, 적재에 성공한 구역 수를 돌려준다.
     */
    public int rollup(LocalDate date) {
        List<Long> zoneIds = sensorInfluxRepository.findZoneIds(
                ZoneDailySummary.startOfDay(date),
                ZoneDailySummary.startOfDay(date.plusDays(1))
        );

        if (zoneIds.isEmpty()) {
            log.warn("적재할 구역이 없습니다. date={}", date);

            return 0;
        }

        int savedCount = 0;

        for (Long zoneId : zoneIds) {
            try {
                zoneDailySummaryInfluxRepository.save(
                        zoneDailySummaryService.summarize(zoneId, date)
                );

                savedCount++;

            } catch (RuntimeException exception) {
                log.error(
                        "구역 요약 적재에 실패해 건너뜁니다. zoneId={}, date={}",
                        zoneId,
                        date,
                        exception
                );
            }
        }

        log.info(
                "하루 요약을 적재했습니다. date={}, 대상={}개 구역, 성공={}개 구역",
                date,
                zoneIds.size(),
                savedCount
        );

        return savedCount;
    }

    /**
     * 구역의 하루 요약을 조회한다.
     */
    public ZoneDailySummary find(
            Long zoneId,
            LocalDate date
    ) {
        return zoneDailySummaryInfluxRepository.findByZoneAndDate(zoneId, date)
                .orElseGet(() -> zoneDailySummaryService.summarize(zoneId, date));
    }

    /**
     * 구역의 최근 요약을 날짜 구간으로 조회한다.
     * 적재되지 않은 날짜는 결과에서 빠지므로 반환된 개수가 요청한 일수보다 적을 수 있다.
     */
    public List<ZoneDailySummary> findBetween(
            Long zoneId,
            LocalDate from,
            LocalDate to
    ) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "조회 시작일이 종료일보다 늦습니다. from=" + from + ", to=" + to
            );
        }

        return zoneDailySummaryInfluxRepository.findByZoneBetween(zoneId, from, to);
    }
}
