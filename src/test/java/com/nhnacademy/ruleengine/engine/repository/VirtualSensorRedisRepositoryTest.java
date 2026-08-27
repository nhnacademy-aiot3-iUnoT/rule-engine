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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VirtualSensorRedisRepositoryTest {

    private static final Long ORGANIZATION_ID = 1L;
    private static final String DEVICE_EUI = "device-eui";
    private static final String CONFIG_KEY = "rule-engine:virtual-sensor:config:device-eui";
    private static final String ACTIVE_DEVICES_KEY = "rule-engine:virtual-sensor:active-devices";
    private static final String ORGANIZATION_KEY = "rule-engine:virtual-sensor:organization:1";

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
        stubInsertScript(1L);

        assertTrue(repository.saveIfAbsent(config()));
    }

    @Test
    @DisplayName("이미 설정이 있으면 저장하지 않고 false를 돌려준다")
    void saveIfAbsentReturnsFalseWhenPresent() {
        stubInsertScript(0L);

        assertFalse(repository.saveIfAbsent(config()));
    }

    @Test
    @DisplayName("Redis 응답이 null이어도 false로 처리한다")
    void saveIfAbsentHandlesNull() {
        stubInsertScript(null);

        assertFalse(repository.saveIfAbsent(config()));
    }

    @Test
    @DisplayName("설정이 있을 때만 갱신한다")
    void update() {
        when(valueOperations.setIfPresent(eq(CONFIG_KEY), any())).thenReturn(true);

        assertTrue(repository.update(config()));
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
        stubInsertScript(1L);

        repository.saveIfAbsent(config());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(), captor.capture(), any());

        when(valueOperations.get(CONFIG_KEY)).thenReturn((String) captor.getValue());

        assertEquals(Optional.of(config()), repository.getVirtualSensorConfig(DEVICE_EUI));
    }

    @Test
    @DisplayName("설정이 없으면 빈 Optional을 돌려준다")
    void getReturnsEmptyWhenAbsent() {
        when(valueOperations.get(CONFIG_KEY)).thenReturn(null);

        assertTrue(repository.getVirtualSensorConfig(DEVICE_EUI).isEmpty());
    }

    @Test
    @DisplayName("설정 값이 깨져 있으면 예외를 던진다")
    void getThrowsWhenBroken() {
        when(valueOperations.get(CONFIG_KEY)).thenReturn("not a json");

        assertThrows(
                IllegalStateException.class,
                () -> repository.getVirtualSensorConfig(DEVICE_EUI)
        );
    }

    @Test
    @DisplayName("저장은 설정 키와 조직 목록을 한 스크립트로 함께 다룬다")
    void saveIfAbsentIndexesByOrganization() {
        stubInsertScript(1L);

        repository.saveIfAbsent(config());

        // 활성 목록이 아니라 조직 목록에 넣어야 관리 화면 목록에 나온다
        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(CONFIG_KEY, ORGANIZATION_KEY)),
                any(),
                eq(DEVICE_EUI)
        );
    }

    @Test
    @DisplayName("삭제는 설정 키와 활성·조직 목록을 한 스크립트로 함께 정리한다")
    void deleteRunsScriptOnAllKeys() {
        repository.delete(ORGANIZATION_ID, DEVICE_EUI);

        verify(redisTemplate).execute(
                any(RedisScript.class),
                eq(List.of(CONFIG_KEY, ACTIVE_DEVICES_KEY, ORGANIZATION_KEY)),
                eq(DEVICE_EUI)
        );
    }

    @Test
    @DisplayName("활성화하면 활성 목록에 기기를 넣는다")
    void activate() {
        repository.activate(DEVICE_EUI);

        verify(setOperations).add(ACTIVE_DEVICES_KEY, DEVICE_EUI);
    }

    @Test
    @DisplayName("비활성화하면 활성 목록에서 기기를 뺀다")
    void deactivate() {
        repository.deactivate(DEVICE_EUI);

        verify(setOperations).remove(ACTIVE_DEVICES_KEY, DEVICE_EUI);
    }

    @Test
    @DisplayName("활성 여부를 확인한다")
    void isActive() {
        when(setOperations.isMember(ACTIVE_DEVICES_KEY, DEVICE_EUI)).thenReturn(true);

        assertTrue(repository.isActive(DEVICE_EUI));
    }

    @Test
    @DisplayName("활성 여부 응답이 null이면 비활성으로 본다")
    void isActiveHandlesNull() {
        when(setOperations.isMember(ACTIVE_DEVICES_KEY, DEVICE_EUI)).thenReturn(null);

        assertFalse(repository.isActive(DEVICE_EUI));
    }

    @Test
    @DisplayName("활성 기기 목록을 돌려준다")
    void findAllActiveDeviceEuis() {
        when(setOperations.members(ACTIVE_DEVICES_KEY)).thenReturn(Set.of(DEVICE_EUI, "other-device"));

        assertEquals(Set.of(DEVICE_EUI, "other-device"), repository.findAllActiveDeviceEuis());
    }

    @Test
    @DisplayName("활성 목록이 없으면 빈 Set을 돌려준다")
    void findAllActiveDeviceEuisHandlesNull() {
        when(setOperations.members(ACTIVE_DEVICES_KEY)).thenReturn(null);

        assertTrue(repository.findAllActiveDeviceEuis().isEmpty());
    }

    @Test
    @DisplayName("조직의 기기 목록을 돌려준다")
    void findDeviceEuisByOrganization() {
        when(setOperations.members(ORGANIZATION_KEY)).thenReturn(Set.of(DEVICE_EUI));

        assertEquals(Set.of(DEVICE_EUI), repository.findDeviceEuisByOrganization(ORGANIZATION_ID));
    }

    @Test
    @DisplayName("조직의 기기 목록이 없으면 빈 Set을 돌려준다")
    void findDeviceEuisByOrganizationHandlesNull() {
        when(setOperations.members(ORGANIZATION_KEY)).thenReturn(null);

        assertTrue(repository.findDeviceEuisByOrganization(ORGANIZATION_ID).isEmpty());
    }

    private void stubInsertScript(Long result) {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), any(), any()))
                .thenReturn(result);
    }

    private VirtualSensorConfig config() {
        return new VirtualSensorConfig(
                ORGANIZATION_ID,
                DEVICE_EUI,
                new VirtualSensorValues(Map.of(
                        SensorType.TEMPERATURE,
                        new SensorValue(GenerationMode.RANGE, 18.0, 26.0, null, null)
                )),
                10L
        );
    }
}
