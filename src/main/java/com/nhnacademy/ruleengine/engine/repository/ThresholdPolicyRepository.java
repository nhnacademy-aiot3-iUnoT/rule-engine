package com.nhnacademy.ruleengine.engine.repository;

import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;

import java.util.Optional;


public interface ThresholdPolicyRepository {
    Optional<ThresholdPolicyDto> findBySection(Long organizationId, Long storageId, Long sectionId);
}
