package com.nhnacademy.ruleengine.engine.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.EnvironmentDecisionState;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

// 센서별 환경상태 판단 상태(EnvironmentDecisionState)를 Redis에 저장해 서버 인스턴스 간 공유한다.
@Repository
@RequiredArgsConstructor
public class EnvironmentDecisionStateRedisRepository {

    private static final String KEY_PREFIX = "rule-engine:env-status:state:";
    // 센서가 더 이상 데이터를 보내지 않을 경우 상태가 영구히 남지 않도록 TTL을 둔다.
    private static final Duration STATE_TTL = Duration.ofDays(3);

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    public Optional<EnvironmentDecisionState> find(String key) {
        String value = redisTemplate.opsForValue().get(getStateKey(key));
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, EnvironmentDecisionState.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "환경상태 판단 상태 역직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    public void save(String key, EnvironmentDecisionState state) {
        try {
            String value = objectMapper.writeValueAsString(state);
            redisTemplate.opsForValue().set(getStateKey(key), value, STATE_TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "환경상태 판단 상태 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    private String getStateKey(String key) {
        return KEY_PREFIX + key;
    }
}
