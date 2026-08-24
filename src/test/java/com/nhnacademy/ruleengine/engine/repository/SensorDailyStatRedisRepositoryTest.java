package com.nhnacademy.ruleengine.engine.repository;

import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyThresholdStat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorDailyStatRedisRepositoryTest {

    private static final Long ZONE_ID = 3L;
    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);
    private static final String KEY = "rule-engine:sensor-daily-stat:3:2026-08-24";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private SensorDailyStatRedisRepository repository;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        repository = new SensorDailyStatRedisRepository(redisTemplate);
    }

    @Test
    @DisplayName("위반이면 total과 out을 모두 1 올린다")
    void recordViolation() {
        repository.recordDailyStat(ZONE_ID, DATE, "temperature", true, 18.0, 26.0);

        verify(hashOperations).increment(KEY, "temperature:total", 1L);
        verify(hashOperations).increment(KEY, "temperature:out", 1L);
    }

    @Test
    @DisplayName("정상이면 total만 올리고 out은 0을 더한다")
    void recordNormal() {
        repository.recordDailyStat(ZONE_ID, DATE, "temperature", false, 18.0, 26.0);

        verify(hashOperations).increment(KEY, "temperature:total", 1L);
        verify(hashOperations).increment(KEY, "temperature:out", 0L);
    }

    @Test
    @DisplayName("임계값을 문자열로 저장하고 TTL을 건다")
    void recordThresholds() {
        repository.recordDailyStat(ZONE_ID, DATE, "temperature", false, 18.0, 26.0);

        verify(hashOperations).put(KEY, "temperature:min", "18.0");
        verify(hashOperations).put(KEY, "temperature:max", "26.0");
        verify(redisTemplate).expire(KEY, Duration.ofDays(3));
    }

    @Test
    @DisplayName("설정되지 않은 경계는 필드를 만들지 않는다")
    void skipNullThreshold() {
        repository.recordDailyStat(ZONE_ID, DATE, "temperature", false, null, 26.0);

        verify(hashOperations, never()).put(eq(KEY), eq("temperature:min"), any());
        verify(hashOperations).put(KEY, "temperature:max", "26.0");
    }

    @Test
    @DisplayName("센서 타입별로 통계를 묶어서 돌려준다")
    void findByZoneAndDate() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("temperature:total", "100");
        entries.put("temperature:out", "7");
        entries.put("temperature:min", "18.0");
        entries.put("temperature:max", "26.0");
        entries.put("humidity:total", "50");
        entries.put("humidity:out", "0");

        when(hashOperations.entries(KEY)).thenReturn(entries);

        Map<String, SensorDailyThresholdStat> stats = repository.findByZoneAndDate(ZONE_ID, DATE);

        SensorDailyThresholdStat temperature = stats.get("temperature");
        SensorDailyThresholdStat humidity = stats.get("humidity");

        assertAll(
                () -> assertEquals(2, stats.size()),
                () -> assertEquals(100L, temperature.total()),
                () -> assertEquals(7L, temperature.outOfRange()),
                () -> assertEquals(18.0, temperature.min()),
                () -> assertEquals(26.0, temperature.max()),
                () -> assertEquals(50L, humidity.total()),
                () -> assertEquals(0L, humidity.outOfRange()),
                () -> assertNull(humidity.min()),
                () -> assertNull(humidity.max())
        );
    }

    @Test
    @DisplayName("통계가 없는 날은 빈 Map을 돌려준다")
    void findByZoneAndDateReturnsEmptyMap() {
        when(hashOperations.entries(KEY)).thenReturn(Map.of());

        assertTrue(repository.findByZoneAndDate(ZONE_ID, DATE).isEmpty());
    }

    @Test
    @DisplayName("구분자가 없는 항목은 건너뛴다")
    void skipUnknownField() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("brokenField", "1");
        entries.put("temperature:total", "10");

        when(hashOperations.entries(KEY)).thenReturn(entries);

        Map<String, SensorDailyThresholdStat> stats = repository.findByZoneAndDate(ZONE_ID, DATE);

        assertAll(
                () -> assertEquals(1, stats.size()),
                () -> assertEquals(10L, stats.get("temperature").total())
        );
    }

    @Test
    @DisplayName("숫자가 아닌 값은 개수 0, 임계값 null로 처리한다")
    void handleNonNumericValues() {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("temperature:total", "많음");
        entries.put("temperature:min", "낮음");

        when(hashOperations.entries(KEY)).thenReturn(entries);

        SensorDailyThresholdStat stat = repository.findByZoneAndDate(ZONE_ID, DATE).get("temperature");

        assertAll(
                () -> assertEquals(0L, stat.total()),
                () -> assertEquals(0L, stat.outOfRange()),
                () -> assertNull(stat.min())
        );
    }
}
