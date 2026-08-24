package com.nhnacademy.ruleengine.engine.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentDecisionState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EnvironmentDecisionStateRedisRepositoryTest {

    private static final String ZONE_KEY = "1:2:3";
    private static final String SENSOR_FIELD = "device-eui:temperature";
    private static final String STATE_KEY = "rule-engine:env-status:state:" + ZONE_KEY;
    private static final Instant MEASURED_AT = Instant.parse("2026-08-24T00:00:00Z");

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    private ObjectMapper objectMapper;
    private EnvironmentDecisionStateRedisRepository repository;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        repository = new EnvironmentDecisionStateRedisRepository(objectMapper, redisTemplate);
    }

    @Test
    @DisplayName("저장한 상태를 그대로 다시 읽을 수 있다")
    void saveAndFindRoundTrip(){
        EnvironmentDecisionState state =
                new EnvironmentDecisionState(EnvStatus.CRITICAL, MEASURED_AT, MEASURED_AT, MEASURED_AT);

        repository.save(ZONE_KEY, SENSOR_FIELD, state);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(hashOperations).put(eq(STATE_KEY), eq(SENSOR_FIELD), captor.capture());

        when(hashOperations.get(STATE_KEY, SENSOR_FIELD)).thenReturn(captor.getValue());

        assertEquals(Optional.of(state), repository.find(ZONE_KEY, SENSOR_FIELD));
    }

    @Test
    @DisplayName("저장할 때마다 TTL을 다시 건다")
    void saveRefreshesTtl() {
        repository.save(ZONE_KEY, SENSOR_FIELD, normalState());

        verify(redisTemplate).expire(STATE_KEY, Duration.ofDays(3));
    }

    @Test
    @DisplayName("저장된 값이 없으면 빈 Optional을 돌려준다")
    void findReturnsEmptyWhenAbsent() {
        when(hashOperations.get(STATE_KEY, SENSOR_FIELD)).thenReturn(null);

        assertTrue(repository.find(ZONE_KEY, SENSOR_FIELD).isEmpty());
    }

    @Test
    @DisplayName("값이 깨져 있으면 상태가 없는 것으로 보고 처음부터 다시 판단하게 한다")
    void findReturnsEmptyWhenBroken() {
        when(hashOperations.get(STATE_KEY, SENSOR_FIELD)).thenReturn("not a json");

        assertTrue(repository.find(ZONE_KEY, SENSOR_FIELD).isEmpty());
    }

    @Test
    @DisplayName("구역의 센서 상태를 한 번에 읽는다")
    void findSensorStatesByZone() throws Exception {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("device-a:temperature", objectMapper.writeValueAsString(criticalState()));
        entries.put("device-a:humidity", objectMapper.writeValueAsString(normalState()));

        when(hashOperations.entries(STATE_KEY)).thenReturn(entries);

        Map<String, EnvironmentDecisionState> states = repository.findSensorStatesByZone(ZONE_KEY);

        assertAll(
                () -> assertEquals(2, states.size()),
                () -> assertEquals(EnvStatus.CRITICAL, states.get("device-a:temperature").state()),
                () -> assertEquals(EnvStatus.NORMAL, states.get("device-a:humidity").state())
        );
    }

    @Test
    @DisplayName("깨진 센서 값 하나 때문에 나머지 집계가 멈추지 않는다")
    void findSensorStatesByZoneSkipsBrokenValue() throws Exception {
        Map<Object, Object> entries = new LinkedHashMap<>();
        entries.put("device-a:temperature", "not a json");
        entries.put("device-a:humidity", objectMapper.writeValueAsString(normalState()));

        when(hashOperations.entries(STATE_KEY)).thenReturn(entries);

        Map<String, EnvironmentDecisionState> states = repository.findSensorStatesByZone(ZONE_KEY);

        assertAll(
                () -> assertEquals(1, states.size()),
                () -> assertFalse(states.containsKey("device-a:temperature")),
                () -> assertEquals(EnvStatus.NORMAL, states.get("device-a:humidity").state())
        );
    }

    @Test
    @DisplayName("저장된 상태가 없으면 빈 Map을 돌려준다")
    void findSensorStatesByZoneReturnsEmptyMap() {
        when(hashOperations.entries(STATE_KEY)).thenReturn(Map.of());

        assertTrue(repository.findSensorStatesByZone(ZONE_KEY).isEmpty());
    }

    @Test
    @DisplayName("직렬화에 실패하면 예외를 던져 잘못된 값이 저장되지 않게 한다")
    void saveThrowsWhenSerializationFails() throws Exception {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any()))
                .thenThrow(new com.fasterxml.jackson.core.JsonProcessingException("실패") {
                });

        EnvironmentDecisionStateRedisRepository failingRepository =
                new EnvironmentDecisionStateRedisRepository(failingMapper, redisTemplate);

        EnvironmentDecisionState state = normalState();

        assertThrows(
                IllegalStateException.class,
                () -> failingRepository.save(ZONE_KEY, SENSOR_FIELD, state)
        );

        verify(hashOperations, never()).put(any(), any(), any());
    }

    @Test
    @DisplayName("구역 상태를 삭제하면 구역 키 전체가 지워진다")
    void deleteZoneStates() {
        repository.deleteZoneStates(ZONE_KEY);

        verify(redisTemplate).delete(STATE_KEY);
    }

    private EnvironmentDecisionState normalState() {
        return new EnvironmentDecisionState(EnvStatus.NORMAL, null, null, MEASURED_AT);
    }

    private EnvironmentDecisionState criticalState() {
        return new EnvironmentDecisionState(EnvStatus.CRITICAL, MEASURED_AT, MEASURED_AT, MEASURED_AT);
    }
}
