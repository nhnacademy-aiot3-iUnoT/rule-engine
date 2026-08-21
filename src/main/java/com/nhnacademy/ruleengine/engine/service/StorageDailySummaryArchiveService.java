package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.environment.StorageDailySummary;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.repository.DailySummaryInfluxRepository;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class StorageDailySummaryArchiveService {

    private final ZoneDailySummaryService zoneDailySummaryService;
    private final DailySummaryInfluxRepository dailySummaryInfluxRepository;
    private final SensorInfluxRepository sensorInfluxRepository;

    /**
     * 해당 날짜에 데이터가 들어온 모든 저장소의 요약을 저장하고, 적재한 구역 수를 리턴한다
     */
    public int rollup(LocalDate date) {
        List<Long> storageIds = sensorInfluxRepository.findStorageIds(
                ZoneDailySummary.startOfDay(date),
                ZoneDailySummary.startOfDay(date.plusDays(1))
        );

        if (storageIds.isEmpty()) {
            log.warn("적재할 저장소가 없습니다. date={}", date);

            return 0;
        }

        int savedZoneCount = 0;

        for (Long storageId : storageIds) {
            try {
                savedZoneCount += rollupStorage(storageId, date);

            } catch (RuntimeException exception) {
                log.error(
                        "저장소 요약 적재에 실패해 건너뜁니다. storageId={}, date={}",
                        storageId,
                        date,
                        exception
                );
            }
        }

        log.info(
                "하루 요약을 적재했습니다. date={}, 저장소={}개, 성공={}개 구역",
                date,
                storageIds.size(),
                savedZoneCount
        );

        return savedZoneCount;
    }

    /**
     * 저장소 하나의 하루 요약을 만들어 저장하고, 담긴 구역 수를 돌려준다.
     */
    private int rollupStorage(
            Long storageId,
            LocalDate date
    ) {
        StorageDailySummary summary = summarize(storageId, date);

        dailySummaryInfluxRepository.save(summary);

        return summary.zones().size();
    }

    /**
     * 저장소의 하루 요약을 날짜 구간(from 이상 to 이하)으로 조회한다.
     * 적재되지 않은 날짜는 결과에서 빠지므로 반환된 개수가 요청한 일수보다 적을 수 있다.
     */
    public List<StorageDailySummary> findBetween(
            Long storageId,
            LocalDate from,
            LocalDate to
    ) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "조회 시작일이 종료일보다 늦습니다. from=" + from + ", to=" + to
            );
        }

        return dailySummaryInfluxRepository.findByStorageBetween(storageId, from, to);
    }

    // 저장본이 없는 날짜를 원본에서 즉석으로 계산한다. 구역 하나가 실패하면 그 구역만 빠진다.
    private StorageDailySummary summarize(
            Long storageId,
            LocalDate date
    ) {
        List<Long> zoneIds = sensorInfluxRepository.findZoneIdsByStorage(
                storageId,
                ZoneDailySummary.startOfDay(date),
                ZoneDailySummary.startOfDay(date.plusDays(1))
        );

        List<ZoneDailySummary> zones = new ArrayList<>();

        for (Long zoneId : zoneIds) {
            try {
                zones.add(zoneDailySummaryService.summarize(zoneId, date));

            } catch (RuntimeException exception) {
                log.error(
                        "구역 요약 계산에 실패해 건너뜁니다. storageId={}, zoneId={}, date={}",
                        storageId,
                        zoneId,
                        date
                );
            }
        }

        return new StorageDailySummary(storageId, date, List.copyOf(zones));
    }
}
