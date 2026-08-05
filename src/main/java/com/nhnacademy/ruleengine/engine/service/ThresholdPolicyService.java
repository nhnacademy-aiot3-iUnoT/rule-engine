package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.repository.ThresholdPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThresholdPolicyService {
    private final ThresholdPolicyRepository thresholdPolicyRepository;

    public ThresholdPolicyDto getThresholdPolicy(Long organizationId, Long storageId, Long sectionId) {
        return thresholdPolicyRepository.findBySection(organizationId, storageId, sectionId)
                .orElseGet(() -> getDefaultMedicineThresholdPolicy(organizationId, storageId, sectionId));
    }

    private ThresholdPolicyDto getDefaultMedicineThresholdPolicy(Long organizationId, Long storageId, Long sectionId) {
        return new ThresholdPolicyDto(
                organizationId,
                storageId,
                sectionId,
                Map.of(
                        SensorType.TEMPERATURE.value(), new ThresholdRange(0.0, 10.0),
                        SensorType.HUMIDITY.value(), new ThresholdRange(10.0, 70.0),
                        SensorType.ILLUMINATION.value(), new ThresholdRange(0.0, 100.0)
                ), 1
        );
    }
}
