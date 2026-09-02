package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.ZoneActivationResponse;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ZoneResolverTest {

    private static final String DEVICE_EUI = "device-eui";
    private static final Long ZONE_ID = 3L;

    private static final ResolvedZoneResponse LOCATION =
            new ResolvedZoneResponse(1L, 2L, ZONE_ID);

    @Mock
    private CachedZoneLookup cachedZoneLookup;

    @InjectMocks
    private ZoneResolver zoneResolver;

    @Test
    @DisplayName("활성 구역에 등록된 기기는 위치를 돌려준다")
    void resolveActiveReturnsLocation() {
        when(cachedZoneLookup.find(DEVICE_EUI)).thenReturn(Optional.of(LOCATION));
        when(cachedZoneLookup.findActivation(ZONE_ID))
                .thenReturn(Optional.of(new ZoneActivationResponse(ZONE_ID, true, true, true)));

        assertEquals(Optional.of(LOCATION), zoneResolver.resolveActive(DEVICE_EUI));
    }

    @Test
    @DisplayName("구역이 비활성이면 위치를 돌려주지 않는다")
    void resolveActiveDropsInactiveZone() {
        when(cachedZoneLookup.find(DEVICE_EUI)).thenReturn(Optional.of(LOCATION));
        when(cachedZoneLookup.findActivation(ZONE_ID))
                .thenReturn(Optional.of(new ZoneActivationResponse(ZONE_ID, false, false, true)));

        assertTrue(zoneResolver.resolveActive(DEVICE_EUI).isEmpty());
    }

    @Test
    @DisplayName("저장소가 비활성이면 구역이 ACTIVE여도 위치를 돌려주지 않는다")
    void resolveActiveDropsInactiveStorage() {
        when(cachedZoneLookup.find(DEVICE_EUI)).thenReturn(Optional.of(LOCATION));
        when(cachedZoneLookup.findActivation(ZONE_ID))
                .thenReturn(Optional.of(new ZoneActivationResponse(ZONE_ID, false, true, false)));

        assertTrue(zoneResolver.resolveActive(DEVICE_EUI).isEmpty());
    }

    @Test
    @DisplayName("구역에 등록되지 않은 기기는 활성 여부를 묻지 않고 버린다")
    void resolveActiveDropsUnregisteredDevice() {
        when(cachedZoneLookup.find(DEVICE_EUI)).thenReturn(Optional.empty());

        assertTrue(zoneResolver.resolveActive(DEVICE_EUI).isEmpty());
    }

    @Test
    @DisplayName("인벤토리 장애로 활성 여부를 못 물으면 활성으로 본다")
    void isZoneActiveFailsOpen() {
        when(cachedZoneLookup.findActivation(ZONE_ID))
                .thenThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "인벤토리 호출 실패"));

        assertTrue(zoneResolver.isZoneActive(ZONE_ID));
    }

    @Test
    @DisplayName("없는 구역은 비활성으로 본다")
    void isZoneActiveTreatsMissingZoneAsInactive() {
        when(cachedZoneLookup.findActivation(ZONE_ID)).thenReturn(Optional.empty());

        assertFalse(zoneResolver.isZoneActive(ZONE_ID));
    }
}
