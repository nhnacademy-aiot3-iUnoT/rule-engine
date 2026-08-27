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

@Repository
@RequiredArgsConstructor
public class VirtualSensorRedisRepository {
    private static final String ACTIVE_DEVICES_KEY = "rule-engine:virtual-sensor:active-devices";
    private static final String CONFIG_KEY_PREFIX = "rule-engine:virtual-sensor:config:";
    private static final String ORGANIZATION_KEY_PREFIX = "rule-engine:virtual-sensor:organization:";

    // 설정을 지울 때 활성 목록과 조직 목록에서도 함께 빼야 유령 기기가 남지 않는다.
    private static final String DELETE_SCRIPT = """
            local deleted = redis.call('DEL', KEYS[1])
            redis.call('SREM', KEYS[2], ARGV[1])
            redis.call('SREM', KEYS[3], ARGV[1])
            return deleted
            """;

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    // 가상 센서 설정 Redis 에 저장
    public boolean saveIfAbsent(VirtualSensorConfig config) {
        Boolean saved = redisTemplate.opsForValue()
                .setIfAbsent(getConfigKey(config.deviceEui()), serialize(config));

        if (!Boolean.TRUE.equals(saved)) {
            return false;
        }

        redisTemplate.opsForSet()
                .add(getOrganizationKey(config.organizationId()), config.deviceEui());

        return true;
    }

    public boolean update(VirtualSensorConfig config) {
        Boolean updated = redisTemplate.opsForValue()
                .setIfPresent(getConfigKey(config.deviceEui()), serialize(config));

        return Boolean.TRUE.equals(updated);
    }

    public void delete(Long organizationId, String deviceEui) {
        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>(DELETE_SCRIPT, Long.class);

        redisTemplate.execute(
                script,
                List.of(
                        getConfigKey(deviceEui),
                        ACTIVE_DEVICES_KEY,
                        getOrganizationKey(organizationId)
                ),
                deviceEui
        );
    }

    // deviceEui로 가상센서 설정 조회
    public Optional<VirtualSensorConfig> getVirtualSensorConfig(String deviceEui) {

        String value = redisTemplate.opsForValue().get(getConfigKey(deviceEui));
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

    public void activate(String deviceEui) {
        redisTemplate.opsForSet()
                .add(ACTIVE_DEVICES_KEY, deviceEui);
    }

    public void deactivate(String deviceEui) {
        redisTemplate.opsForSet()
                .remove(ACTIVE_DEVICES_KEY, deviceEui);
    }

    public boolean isActive(String deviceEui) {
        Boolean active = redisTemplate.opsForSet()
                .isMember(ACTIVE_DEVICES_KEY, deviceEui);

        return Boolean.TRUE.equals(active);
    }

    public Set<String> findAllActiveDeviceEuis() {
        Set<String> members = redisTemplate.opsForSet()
                .members(ACTIVE_DEVICES_KEY);

        return members == null ? Set.of() : members;
    }

    // 조직의 가상 센서 목록. 관리 화면이 조직 단위로 보여주므로 조직별 색인을 따로 둔다.
    public Set<String> findDeviceEuisByOrganization(Long organizationId) {
        Set<String> members = redisTemplate.opsForSet()
                .members(getOrganizationKey(organizationId));

        return members == null ? Set.of() : members;
    }

    private String serialize(VirtualSensorConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "가상 센서 설정 직렬화에 실패했습니다.",
                    exception
            );
        }
    }

    private String getConfigKey(String deviceEui) {
        return CONFIG_KEY_PREFIX + deviceEui;
    }

    private String getOrganizationKey(Long organizationId) {
        return ORGANIZATION_KEY_PREFIX + organizationId;
    }
}
