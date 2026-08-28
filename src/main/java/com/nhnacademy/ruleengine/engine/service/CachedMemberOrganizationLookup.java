package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.global.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.inventory.MemberOrganizationResponse;
import com.nhnacademy.ruleengine.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class CachedMemberOrganizationLookup {

    private final InventoryClient inventoryClient;

    @Cacheable(cacheNames = CacheConfig.MEMBER_ORGANIZATION, key = "#accountUuid")
    public Optional<MemberOrganizationResponse> find(UUID accountUuid) {
        Optional<MemberOrganizationResponse> membership = inventoryClient.findMemberOrganization(accountUuid);

        if (membership.isEmpty()) {
            log.warn("조직에 소속되지 않은 계정입니다. accountUuid={}", accountUuid);
        }

        return membership;
    }
}
