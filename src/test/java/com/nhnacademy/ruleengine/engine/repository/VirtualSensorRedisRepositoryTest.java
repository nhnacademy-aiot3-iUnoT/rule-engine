package com.nhnacademy.ruleengine.engine.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.virtual.GenerationMode;
import com.nhnacademy.ruleengine.engine.dto.virtual.SensorValue;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VirtualSensorRedisRepositoryTest {

    private static final Long ZONE_ID = 3L;
    private static final String CONFIG_KEY = "rule-engine:virtual-sensor:config:3";
    private static final String ACTIVE_ZONES_KEY = "rule-engine:virtual-sensor:active-zones";

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private ObjectMapper objectMapper;
    private VirtualSensorRedisRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        repository = new VirtualSensorRedisRepository(objectMapper, redisTemplate);
    }

    @Test
    @DisplayName("설정이 없을 때만 저장하고 저장 여부를 돌려준다")
    void saveIfAbsent() {
        when(valueOperations.setIfAbsent(eq(CONFIG_KEY), any())).thenReturn(true);

        assertTrue(repository.saveIfAbsent(config()));
    }

    @Test
    @DisplayName("이미 설정이 있으면 저장하지 않고 false를 돌려준다")
    void saveIfAbsentReturnsFalseWhenPresent() {
        when(valueOperations.setIfAbsent(eq(CONFIG_KEY), any())).thenReturn(false);

        assertFalse(repository.saveIfAbsent(config()));
    }

    @Test
    @DisplayName("Redis 응답이 null이어도 false로 처리한다")
    void saveIfAbsentHandlesNull() {
        when(valueOperations.setIfAbsent(eq(CONFIG_KEY), any())).thenReturn(null);

        assertFalse(repository.saveIfAbsent(config()));
    }

    @Test
    @DisplayName("설정이 있을 때만 갱신한다")
    void update() {
        when(valueOperations.setIfPresent(eq(CONFIG_KEY), any())).thenReturn(true);

        assertTrue(repository.update(config()));
        verify(valueOperations, never()).setIfAbsent(any(), any());
    }

    @Test
    @DisplayName("설정이 없으면 갱신하지 않고 false를 돌려준다")
    void updateReturnsFalseWhenAbsent() {
        when(valueOperations.setIfPresent(eq(CONFIG_KEY), any())).thenReturn(false);

        assertFalse(repository.update(config()));
    }

    @Test
    @DisplayName("저장한 설정을 그대로 다시 읽을 수 있다")
    void saveAndGetRoundTrip() {
        when(valueOperations.setIfAbsent(eq(CONFIG_KEY), any())).thenReturn(true);

        repository.saveIfAbsent(config());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).setIfAbsent(eq(CONFIG_KEY), captor.capture());

        when(valueOperations.get(CONFIG_KEY)).thenReturn(captor.getValue());

        assertEquals(Optional.of(config()), repository.getVirtualSensorConfig(ZONE_ID));
    }

    @Test
    @DisplayName("설정이 없으면 빈 Optional을 돌려준다")
    void getReturnsEmptyWhenAbsent() {
        when(valueOperations.get(CONFIG_KEY)).thenReturn(null);

        assertTrue(repository.getVirtualSensorConfig(ZONE_ID).isEmpty());
    }

    @Test
    @DisplayName("설정 값이 깨져 있으면 예외를 던진다")
    void getThrowsWhenBroken() {
        when(valueOperations.get(CONFIG_KEY)).thenReturn("not a json");

        assertThrows(
                IllegalStateException.class,
                () -> repository.getVirtualSensorConfig(ZONE_ID)
        );
    }

    @Test
    @DisplayName("삭제는 설정 키와 활성 목록을 한 스크립트로 함께 정리한다")
    void deleteRunsScriptOnBothKeys() {
        repository.delete(ZONE_ID);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(CONFIG_KEY, ACTIVE_ZONES_KEY)),
                eq("3")
        );
    }

    @Test
    @DisplayName("활성화하면 활성 목록에 구역을 넣는다")
    void activate() {
        repository.activate(ZONE_ID);

        verify(setOperations).add(ACTIVE_ZONES_KEY, "3");
    }

    @Test
    @DisplayName("비활성화하면 활성 목록에서 구역을 뺀다")
    void deactivate() {
        repository.deactivate(ZONE_ID);

        verify(setOperations).remove(ACTIVE_ZONES_KEY, "3");
    }

    @Test
    @DisplayName("활성 여부를 확인한다")
    void isActive() {
        when(setOperations.isMember(ACTIVE_ZONES_KEY, "3")).thenReturn(true);

        assertTrue(repository.isActive(ZONE_ID));
    }

    @Test
    @DisplayName("활성 여부 응답이 null이면 비활성으로 본다")
    void isActiveHandlesNull() {
        when(setOperations.isMember(ACTIVE_ZONES_KEY, "3")).thenReturn(null);

        assertFalse(repository.isActive(ZONE_ID));
    }

    @Test
    @DisplayName("활성 구역 목록을 숫자로 바꿔 돌려준다")
    void findAllActiveZoneIds() {
        when(setOperations.members(ACTIVE_ZONES_KEY)).thenReturn(Set.of("3", "7"));

        assertEquals(Set.of(3L, 7L), repository.findAllActiveZoneIds());
    }

    @Test
    @DisplayName("활성 목록이 없으면 빈 Set을 돌려준다")
    void findAllActiveZoneIdsHandlesNull() {
        when(setOperations.members(ACTIVE_ZONES_KEY)).thenReturn(null);

        assertTrue(repository.findAllActiveZoneIds().isEmpty());
    }

    private VirtualSensorConfig config() {
        return new VirtualSensorConfig(
                1L,
                2L,
                ZONE_ID,
                "device-eui",
                new VirtualSensorValues(Map.of(
                        SensorType.TEMPERATURE,
                        new SensorValue(GenerationMode.RANGE, 18.0, 26.0, null, null)
                )),
                10L
        );
    }
}
