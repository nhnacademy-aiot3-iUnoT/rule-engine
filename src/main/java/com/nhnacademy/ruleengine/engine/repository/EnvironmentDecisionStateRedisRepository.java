package com.nhnacademy.ruleengine.engine.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentDecisionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// 센서별 환경상태 판단 상태(EnvironmentDecisionState)를 Redis에 저장해 서버 인스턴스 간 공유한다.
// 구역 단위 Hash(key=구역, field=센서)로 저장하는데, 인벤토리의 구역 환경 상태를 계산할 때
// 같은 구역 센서들의 상태를 한 번에 읽어야 하기 때문이다.
@Slf4j
@Repository
@RequiredArgsConstructor
public class EnvironmentDecisionStateRedisRepository {

    // v2: 타임스탬프를 LocalDateTime -> Instant로 바꾸면서 옛 데이터와 형식이 달라져 키를 분리했다.
    private static final String KEY_PREFIX = "rule-engine:env-status:state:";
    // 센서가 더 이상 데이터를 보내지 않을 경우 상태가 영구히 남지 않도록 TTL을 둔다.
    private static final Duration STATE_TTL = Duration.ofDays(3);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public Optional<EnvironmentDecisionState> find(String zoneKey, String sensorField) {
        Object value = redisTemplate.opsForHash().get(getStateKey(zoneKey), sensorField);
        if (value == null) {
            return Optional.empty();
        }

        // 값 하나가 깨졌다고 측정값 수집까지 막을 수는 없다. 상태가 없는 것으로 보고 처음부터 다시 판단한다.
        try {
            return Optional.of(
                    objectMapper.readValue(String.valueOf(value), EnvironmentDecisionState.class)
            );
        } catch (JsonProcessingException exception) {
            log.warn("환경상태 판단 상태 역직렬화에 실패해 상태 없이 처리합니다. zoneKey={}, sensor={}",
                    zoneKey, sensorField, exception);
            return Optional.empty();
        }
    }

    // 구역에 속한 센서들의 상태를 모두 읽는다. 값 하나가 깨져도 나머지 집계는 계속되어야 하므로 건너뛴다.
    public Map<String, EnvironmentDecisionState> findSensorStatesByZone(String zoneKey) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(getStateKey(zoneKey));

        Map<String, EnvironmentDecisionState> states = new LinkedHashMap<>();

        for (Map.Entry<Object, Object> entry : entries.entrySet()) {
            String sensorField = String.valueOf(entry.getKey());

            try {
                states.put(
                        sensorField,
                        objectMapper.readValue(String.valueOf(entry.getValue()), EnvironmentDecisionState.class)
                );
            } catch (JsonProcessingException exception) {
                log.warn("환경상태 판단 상태 역직렬화에 실패해 건너뜁니다. zoneKey={}, sensor={}",
                        zoneKey, sensorField, exception);
            }
        }

        return states;
    }

    public void save(String zoneKey, String sensorField, EnvironmentDecisionState state) {
        try {
            String value = objectMapper.writeValueAsString(state);
            String key = getStateKey(zoneKey);

            redisTemplate.opsForHash().put(key, sensorField, value);
            redisTemplate.expire(key, STATE_TTL);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "환경상태 판단 상태 직렬화에 실패했습니다.",
                    exception
            );
        }
    }


    public void deleteZoneStates(String zoneKey) {
        redisTemplate.delete(getStateKey(zoneKey));
    }

    private String getStateKey(String zoneKey) {
        return KEY_PREFIX + zoneKey;
    }
}
