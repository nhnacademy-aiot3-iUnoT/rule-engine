package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.global.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.ZoneActivationResponse;
import com.nhnacademy.ruleengine.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedZoneLookup {

    private final InventoryClient inventoryClient;

    @Cacheable(cacheNames = CacheConfig.DEVICE_ZONE, key = "#deviceEui")
    public Optional<ResolvedZoneResponse> find(String deviceEui) {
        Optional<ResolvedZoneResponse> zone = inventoryClient.findZoneResponse(deviceEui);

        if (zone.isEmpty()) {
            log.warn("등록되지 않은 기기라 구역 정보가 없습니다. deviceEui={}", deviceEui);
        }

        return zone;
    }

    @Cacheable(cacheNames = CacheConfig.ZONE_ACTIVATION, key = "#zoneId")
    public Optional<ZoneActivationResponse> findActivation(Long zoneId) {
        return inventoryClient.findZoneActivation(zoneId);
    }

    @Cacheable(cacheNames = CacheConfig.ZONE_LOCATION, key = "#zoneId")
    public Optional<ResolvedZoneResponse> findLocation(Long zoneId) {
        Optional<ResolvedZoneResponse> zone = inventoryClient.findZoneLocation(zoneId);

        if (zone.isEmpty()) {
            log.warn("존재하지 않는 구역입니다. zoneId={}", zoneId);
        }

        return zone;
    }
}
