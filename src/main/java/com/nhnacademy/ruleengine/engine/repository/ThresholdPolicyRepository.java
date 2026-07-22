package com.nhnacademy.ruleengine.engine.repository;

import com.nhnacademy.ruleengine.engine.dto.ThresholdPolicyDto;

import java.util.Optional;


public interface ThresholdPolicyRepository {
    Optional<ThresholdPolicyDto> findBySection(Long organizationId, Long storageId, Long sectionId);
}
