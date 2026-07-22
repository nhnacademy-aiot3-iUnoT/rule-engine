package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.repository.ThresholdPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdPolicyService {
    private final ThresholdPolicyRepository thresholdPolicyRepository;

    public ThresholdPolicyDto getThresholdPolicy(Long organizationId, Long storageId, Long sectionId){
        return thresholdPolicyRepository.findBySection(organizationId, storageId, sectionId)
                .orElseGet(() -> getDefaultMedicineThresholdPolicy(organizationId, storageId, sectionId));
    }

    private ThresholdPolicyDto getDefaultMedicineThresholdPolicy(Long organizationId, Long storageId, Long sectionId) {
        log.info("환경 임계값이 설정되지않아, 기본값을 사용합니다. organizationId={}, locationId={}, positionId={}", organizationId, storageId, sectionId);
        return new ThresholdPolicyDto(
                organizationId,
                storageId,
                sectionId,
                15.0,
                25.0,
                35.0,
                65.0,
                30
        );
    }
}
