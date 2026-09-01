package com.nhnacademy.ruleengine.engine.service;


import com.nhnacademy.ruleengine.global.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdPolicyService {
    private final InventoryClient client;

    // 임계값은 구역에만 종속되고(GET /zones/{zoneId}/zone-threshold) zoneId는 전역 고유하므로
    // zoneId 하나가 곧 캐시 키다.
    @Cacheable(cacheNames = CacheConfig.ZONE_THRESHOLD, key = "#zoneId")
    public ThresholdPolicyDto getThresholdPolicy(Long zoneId) {
        return client.getThresholdPolicy(zoneId);
    }


}
