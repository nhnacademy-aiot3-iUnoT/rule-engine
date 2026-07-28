package com.nhnacademy.ruleengine.engine.repository.impl;

import com.nhnacademy.ruleengine.engine.dto.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.repository.ThresholdPolicyRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

//임시 구현체
@Repository
public class InMemoryThresholdPolicyRepository implements ThresholdPolicyRepository {
    @Override
    public Optional<ThresholdPolicyDto> findBySection(Long organizationId, Long storageId, Long sectionId) {
        return Optional.empty();
    }
}
