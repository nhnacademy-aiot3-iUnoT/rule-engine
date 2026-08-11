package com.nhnacademy.ruleengine.engine.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VirtualSensorRedisRepository {
    private static final String ACTIVE_SECTIONS_KEY = "rule-engine:virtual-sensor:active-sections";
    private static final String CONFIG_KEY_PREFIX = "rule-engine:virtual-sensor:config:";
    private static final String DELETE_SCRIPT = """
            local deleted = redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[1])
            return deleted
            """;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    // 가상 센서 설정 Redis 에 저장
    public boolean saveIfAbsent(VirtualSensorConfig config) {
        try {
            String key = getConfigKey(config.zoneId());
            String value = objectMapper.writeValueAsString(config);

            Boolean saved = redisTemplate.opsForValue()
                    .setIfAbsent(key, value);

            return Boolean.TRUE.equals(saved);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "가상 센서 설정 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    public boolean update(VirtualSensorConfig config) {
        try {
            String key = getConfigKey(config.zoneId());
            String value = objectMapper.writeValueAsString(config);

            Boolean updated = redisTemplate.opsForValue()
                    .setIfPresent(key, value);

            return Boolean.TRUE.equals(updated);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "가상 센서 설정 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    public void delete(Long zoneId) {
        String configKey = getConfigKey(zoneId);

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(DELETE_SCRIPT, Long.class);

        redisTemplate.execute(
                script,
                List.of(configKey, ACTIVE_SECTIONS_KEY),
                zoneId.toString()
        );

    }

    // zoneId로 가상센서 설정 조회
    public Optional<VirtualSensorConfig> getVirtualSensorConfig(Long zoneId) {

        String value = redisTemplate.opsForValue().get(getConfigKey(zoneId));
        if (value == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(value, VirtualSensorConfig.class));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "가상 센서 설정 역직렬화에 실패했습니다.",
                    e
            );
        }
    }

    public void activate(Long zoneId) {
        redisTemplate.opsForSet()
                .add(ACTIVE_SECTIONS_KEY, zoneId.toString());
    }

    public void deactivate(Long zoneId) {
        redisTemplate.opsForSet()
                .remove(ACTIVE_SECTIONS_KEY, zoneId.toString());
    }

    public boolean isActive(Long zoneId) {
        Boolean active = redisTemplate.opsForSet()
                .isMember(ACTIVE_SECTIONS_KEY, zoneId.toString());

        return Boolean.TRUE.equals(active);
    }

    public Set<Long> findAllActiveSectionIds() {
        Set<String> members = redisTemplate.opsForSet()
                .members(ACTIVE_SECTIONS_KEY);

        if (members == null) {
            return Set.of();
        }

        return members.stream()
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private String getConfigKey(Long zoneId) {
        return CONFIG_KEY_PREFIX + zoneId;
    }


}
