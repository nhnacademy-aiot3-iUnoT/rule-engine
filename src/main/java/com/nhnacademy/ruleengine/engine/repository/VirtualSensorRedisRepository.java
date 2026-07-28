package com.nhnacademy.ruleengine.engine.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class VirtualSensorRedisRepository {
    private static final String ACTIVE_SECTIONS_KEY = "rule-engine:virtual-sensor:active-sections";
    private static final String CONFIG_KEY_PREFIX = "rule-engine:virtual-sensor:config:";
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    // 가상 센서 설정 Redis 에 저장
    public boolean saveIfAbsent(VirtualSensorConfig config) {
        try {
            String key = getConfigKey(config.sectionId());
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
            String key = getConfigKey(config.sectionId());
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

    // sectionId로 가상센서 설정 조회
    public Optional<VirtualSensorConfig> getVirtualSensorConfig(Long sectionId) {

        String value = redisTemplate.opsForValue().get(getConfigKey(sectionId));
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

    public void activate(Long sectionId) {
        redisTemplate.opsForSet()
                .add(ACTIVE_SECTIONS_KEY, sectionId.toString());
    }

    public void deactivate(Long sectionId) {
        redisTemplate.opsForSet()
                .remove(ACTIVE_SECTIONS_KEY, sectionId.toString());
    }

    public boolean isActive(Long sectionId) {
        Boolean active = redisTemplate.opsForSet()
                .isMember(ACTIVE_SECTIONS_KEY, sectionId.toString());

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

    private String getConfigKey(Long sectionId) {
        return CONFIG_KEY_PREFIX + sectionId;
    }

}
