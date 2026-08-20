package com.nhnacademy.ruleengine.engine.repository;

import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyThresholdStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 하루 요약이 쓸 임계값 판단 결과를 판단 시점에 Redis로 모은다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorDailyStatRedisRepository {

    private static final String KEY_PREFIX = "rule-engine:sensor-daily-stat:";

    private static final String TOTAL_SUFFIX = ":total";
    private static final String OUT_SUFFIX = ":out";
    private static final String MIN_SUFFIX = ":min";
    private static final String MAX_SUFFIX = ":max";

    /*
     * 적재 배치는 다음 날 자정 직후에 돌지만, 배치가 실패한 날을 원본 보관 기간(이틀) 안에
     * 다시 적재할 수 있어야 하므로 그보다 길게 남긴다.
     */
    private static final Duration STAT_TTL = Duration.ofDays(3);

    private final StringRedisTemplate redisTemplate;

    /**
     * 임계값 판단 한 건을 기록한다.
     * <p>
     * 실패를 삼키지 않는다. 이 실패를 견딜지는 호출하는 쪽이 정할 일이고,
     * 수집 경로에서 부르는 노드가 그 판단을 맡고 있다.
     */
    public void record(
            Long zoneId,
            LocalDate date,
            String sensorType,
            boolean violated,
            Double min,
            Double max
    ) {
        String key = getKey(zoneId, date);

        redisTemplate.opsForHash().increment(key, sensorType + TOTAL_SUFFIX, 1L);
        redisTemplate.opsForHash().increment(key, sensorType + OUT_SUFFIX, violated ? 1L : 0L);

        putThreshold(key, sensorType + MIN_SUFFIX, min);
        putThreshold(key, sensorType + MAX_SUFFIX, max);

        redisTemplate.expire(key, STAT_TTL);
    }

    /**
     * 구역의 하루치 통계를 센서 타입별로 읽는다.
     * <p>
     * 통계가 없는 날은 빈 Map을 돌려준다. 0건으로 채우면 "이탈이 없었다"와
     * "그날 통계를 남기지 못했다"가 구분되지 않는다.
     */
    public Map<String, SensorDailyThresholdStat> findByZoneAndDate(
            Long zoneId,
            LocalDate date
    ) {
        Map<Object, Object> entries =
                redisTemplate.opsForHash().entries(getKey(zoneId, date));

        if (entries.isEmpty()) {
            return Map.of();
        }

        Map<String, Map<String, String>> bySensorType = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String field = String.valueOf(entry.getKey());
            int separator = field.lastIndexOf(':');

            if (separator <= 0) {
                log.warn("알 수 없는 통계 항목이라 건너뜁니다. field={}", field);
                continue;
            }

            bySensorType
                    .computeIfAbsent(field.substring(0, separator), key -> new HashMap<>())
                    .put(field.substring(separator), String.valueOf(entry.getValue()));
        }

        Map<String, SensorDailyThresholdStat> stats = new LinkedHashMap<>();

        bySensorType.forEach((sensorType, values) -> stats.put(
                sensorType,
                new SensorDailyThresholdStat(
                        sensorType,
                        parseLong(values.get(TOTAL_SUFFIX)),
                        parseLong(values.get(OUT_SUFFIX)),
                        parseDouble(values.get(MIN_SUFFIX)),
                        parseDouble(values.get(MAX_SUFFIX))
                )
        ));

        return stats;
    }

    // 설정되지 않은 경계는 필드를 만들지 않는다. 0으로 채우면 "경계 없음"이 "경계가 0"이 된다.
    private void putThreshold(
            String key,
            String field,
            Double value
    ) {
        if (value != null) {
            redisTemplate.opsForHash().put(key, field, String.valueOf(value));
        }
    }

    private long parseLong(String value) {
        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            log.warn("통계 값이 숫자 형식이 아닙니다. value={}", value);

            return 0L;
        }
    }

    private Double parseDouble(String value) {
        if (value == null) {
            return null;
        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            log.warn("임계값이 숫자 형식이 아닙니다. value={}", value);

            return null;
        }
    }

    private String getKey(
            Long zoneId,
            LocalDate date
    ) {
        return KEY_PREFIX + zoneId + ":" + date;
    }
}
