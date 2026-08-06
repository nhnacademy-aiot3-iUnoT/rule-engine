package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.virtual.*;
import com.nhnacademy.ruleengine.engine.exception.VirtualSensorFlowException;
import com.nhnacademy.ruleengine.engine.repository.VirtualSensorRedisRepository;
import com.nhnacademy.ruleengine.engine.validation.LocationHierarchyValidator;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VirtualSensorService {
    private final LocationHierarchyValidator locationHierarchyValidator;
    private final VirtualSensorRedisRepository virtualSensorRedisRepository;

    public void createVirtualSensor(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorCreateRequest request
    ) {
        locationHierarchyValidator.validateSection(organizationId, storageId, sectionId);

        VirtualSensorConfig sensorConfig = VirtualSensorConfig.from(
                organizationId,
                storageId,
                sectionId,
                request
        );

        boolean saved = virtualSensorRedisRepository.saveIfAbsent(sensorConfig);

        if (!saved) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_EXISTS);
        }

        // Coordinator가 활성 목록을 확인한 뒤 Lock 소유 서버에서 Flow를 시작한다.
        virtualSensorRedisRepository.activate(sectionId);
    }

    public VirtualSensorInfoResponse getVirtualSensor(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        locationHierarchyValidator.validateSection(organizationId, storageId, sectionId);

        return VirtualSensorInfoResponse.from(virtualSensorRedisRepository.getVirtualSensorConfig(sectionId)
                .orElseThrow(() -> new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND)));
    }

    public void updateVirtualSensor(
            Long organizationId,
            Long storageId,
            Long zoneId,
            VirtualSensorUpdateRequest request
    ) {
        locationHierarchyValidator.validateSection(organizationId, storageId, zoneId);

        VirtualSensorConfig existing = virtualSensorRedisRepository.getVirtualSensorConfig(zoneId)
                .orElseThrow(() -> new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND));

        VirtualSensorConfig sensorConfig = VirtualSensorConfig.merge(existing, request);

        boolean updated = virtualSensorRedisRepository.update(sensorConfig);

        if (!updated) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND);
        }
    }

    public void changeFlowStatus(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorStatus status
    ) {

        locationHierarchyValidator.validateSection(organizationId, storageId, sectionId);

        virtualSensorRedisRepository.getVirtualSensorConfig(sectionId)
                .orElseThrow(() -> new VirtualSensorFlowException(
                        ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND
                ));

        if (status == VirtualSensorStatus.ACTIVE) {
            virtualSensorRedisRepository.activate(sectionId);
            return;
        }

        if (status == VirtualSensorStatus.INACTIVE) {
            virtualSensorRedisRepository.deactivate(sectionId);
        }
    }
}
