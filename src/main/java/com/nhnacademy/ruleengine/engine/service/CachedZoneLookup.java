package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;

// 구역 조회의 캐싱 계층. ZoneResolver와 분리한 이유가 두 가지다.
//
// 1. 같은 빈 안에서 호출하면 프록시를 타지 않아 @Cacheable이 동작하지 않는다.
// 2. 미등록 기기(404)의 빈 결과만 캐싱하고 인벤토리 장애는 캐싱하면 안 된다.
//    장애 응답이 캐시에 굳으면 인벤토리가 복구된 뒤에도 TTL 내내 멀쩡한 센서 데이터를 버리게 된다.
//    그래서 예외를 여기서 잡지 않고 ZoneResolver까지 그대로 올려보내 캐시에 남기지 않는다.
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
}
