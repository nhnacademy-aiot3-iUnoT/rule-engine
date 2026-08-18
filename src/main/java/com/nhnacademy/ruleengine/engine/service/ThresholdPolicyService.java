package com.nhnacademy.ruleengine.engine.service;


import com.nhnacademy.ruleengine.engine.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdPolicyService {
    private final InventoryClient client;

    public ThresholdPolicyDto getThresholdPolicy(Long organizationId, Long storageId, Long zoneId) {
        return client.getThresholdPolicy(organizationId, storageId, zoneId);
    }


}
