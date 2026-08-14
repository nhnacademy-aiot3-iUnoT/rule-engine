package com.nhnacademy.ruleengine.engine.client;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("deviceEui를 쿼리 파라미터로 붙여 구역 정보를 조회한다")
    void getZoneResponse() {
        // given
        ResolvedZoneResponse expected = new ResolvedZoneResponse(1L, 2L, 3L);

        when(apiClient.get(anyString(), eq(ResolvedZoneResponse.class)))
                .thenReturn(expected);

        // when
        ResolvedZoneResponse response = inventoryClient.getZoneResponse("24e124128c067999");

        // then
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(apiClient).get(url.capture(), eq(ResolvedZoneResponse.class));

        assertAll(
                () -> assertEquals(expected, response),
                () -> assertEquals(
                        BASE_URL + "/api/core/internal/devices/location?device-eui=24e124128c067999",
                        url.getValue()
                )
        );
    }

    @Test
    @DisplayName("등록되지 않은 deviceEui면 ApiException이 그대로 전파된다")
    void getZoneResponseNotFound() {
        // given
        when(apiClient.get(anyString(), eq(ResolvedZoneResponse.class)))
                .thenThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "센서를 찾을 수 없습니다."));

        // when & then
        assertThrows(
                ApiException.class,
                () -> inventoryClient.getZoneResponse("unknown-eui")
        );
    }
}
