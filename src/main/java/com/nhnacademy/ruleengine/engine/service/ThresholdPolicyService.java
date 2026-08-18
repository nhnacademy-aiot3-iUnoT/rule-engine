package com.nhnacademy.ruleengine.engine.service;


import com.nhnacademy.ruleengine.engine.client.InventoryClient;
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

    @Cacheable(cacheNames = CacheConfig.ZONE_THRESHOLD, key = "#zoneId")
    public ThresholdPolicyDto getThresholdPolicy(Long organizationId, Long storageId, Long zoneId) {
        return client.getThresholdPolicy(organizationId, storageId, zoneId);
    }


}
