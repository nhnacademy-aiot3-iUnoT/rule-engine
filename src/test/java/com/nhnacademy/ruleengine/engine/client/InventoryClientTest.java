package com.nhnacademy.ruleengine.engine.client;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.ThresholdSpecResponse;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryClientTest {

    private static final String BASE_URL = "http://localhost:10411";

    @Mock
    private ApiClient apiClient;

    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        inventoryClient = new InventoryClient(apiClient, BASE_URL);
    }

    @Test
    @DisplayName("임계값 응답을 룰 엔진 센서타입 키로 변환하고 임계시간도 센서타입별로 담는다")
    void getThresholdPolicy() {
        // given
        stubThresholds(List.of(
                spec(1L, "TEMPERATURE", BigDecimal.valueOf(20.0), BigDecimal.valueOf(30.0), 5),
                spec(2L, "HUMIDITY", null, BigDecimal.valueOf(70.0), 10),
                spec(3L, "ILLUMINATION", BigDecimal.valueOf(50.0), BigDecimal.valueOf(150.0), 5)
        ));

        // when
        ThresholdPolicyDto policy = inventoryClient.getThresholdPolicy(3L);

        ThresholdRange temperature = policy.rangeFor(SensorType.TEMPERATURE.value()).orElseThrow();
        ThresholdRange humidity = policy.rangeFor(SensorType.HUMIDITY.value()).orElseThrow();

        // then
        assertAll(
                () -> assertEquals(20.0, temperature.min()),
                () -> assertEquals(30.0, temperature.max()),
                () -> assertEquals(5, temperature.alertDurationMinutes()),
                () -> assertNull(humidity.min()),
                () -> assertEquals(70.0, humidity.max()),
                () -> assertEquals(10, humidity.alertDurationMinutes()),
                () -> assertTrue(policy.rangeFor(SensorType.ILLUMINATION.value()).isPresent())
        );
    }

    @Test
    @DisplayName("지원하지 않는 센서 종류의 임계값은 건너뛴다")
    void getThresholdPolicySkipsUnknownSensorType() {
        // given
        stubThresholds(List.of(
                spec(1L, "TEMPERATURE", BigDecimal.valueOf(20.0), BigDecimal.valueOf(30.0), 5),
                spec(9L, "PRESSURE", BigDecimal.valueOf(1.0), BigDecimal.valueOf(2.0), 5)
        ));

        // when
        ThresholdPolicyDto policy = inventoryClient.getThresholdPolicy(3L);

        // then
        assertAll(
                () -> assertEquals(1, policy.ranges().size()),
                () -> assertTrue(policy.rangeFor(SensorType.TEMPERATURE.value()).isPresent())
        );
    }

    @Test
    @DisplayName("설정된 임계값이 없으면 범위가 빈 정책을 돌려준다")
    void getThresholdPolicyWithoutSpecs() {
        // given
        stubThresholds(List.of());

        // when
        ThresholdPolicyDto policy = inventoryClient.getThresholdPolicy(3L);

        // then
        assertTrue(policy.ranges().isEmpty());
    }

    @Test
    @DisplayName("deviceEui를 쿼리 파라미터로 붙여 구역 정보를 조회한다")
    void findZoneResponse() {
        // given
        ResolvedZoneResponse expected = new ResolvedZoneResponse(1L, 2L, 3L);

        when(apiClient.find(anyString(), eq(ResolvedZoneResponse.class)))
                .thenReturn(Optional.of(expected));

        // when
        Optional<ResolvedZoneResponse> response = inventoryClient.findZoneResponse("24e124128c067999");

        // then
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(apiClient).find(url.capture(), eq(ResolvedZoneResponse.class));

        assertAll(
                () -> assertEquals(Optional.of(expected), response),
                () -> assertEquals(
                        BASE_URL + "/api/core/internal/devices/location?device-eui=24e124128c067999",
                        url.getValue()
                )
        );
    }

    @Test
    @DisplayName("등록되지 않은 deviceEui면 빈 값을 돌려준다")
    void findZoneResponseNotFound() {
        // given
        when(apiClient.find(anyString(), eq(ResolvedZoneResponse.class)))
                .thenReturn(Optional.empty());

        // when & then
        assertTrue(inventoryClient.findZoneResponse("unknown-eui").isEmpty());
    }

    @Test
    @DisplayName("인벤토리 호출이 실패하면 ApiException이 그대로 전파된다")
    void findZoneResponseFailure() {
        // given
        when(apiClient.find(anyString(), eq(ResolvedZoneResponse.class)))
                .thenThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "호출 실패"));

        // when & then
        assertThrows(
                ApiException.class,
                () -> inventoryClient.findZoneResponse("unknown-eui")
        );
    }

    @SuppressWarnings("unchecked")
    private void stubThresholds(List<ThresholdSpecResponse> specs) {
        when(apiClient.get(anyString(), any(ParameterizedTypeReference.class))).thenReturn(specs);
    }

    private ThresholdSpecResponse spec(
            Long sensorTypeId,
            String sensorTypeName,
            BigDecimal minValue,
            BigDecimal maxValue,
            Integer alertDuration
    ) {
        return new ThresholdSpecResponse(3L, sensorTypeId, sensorTypeName, minValue, maxValue, alertDuration);
    }
}
