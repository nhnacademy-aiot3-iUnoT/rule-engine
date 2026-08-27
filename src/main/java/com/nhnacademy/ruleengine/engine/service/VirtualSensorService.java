package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorUpdateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorCreateResponse;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorInfoResponse;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorUpdateResponse;
import com.nhnacademy.ruleengine.engine.exception.VirtualSensorFlowException;
import com.nhnacademy.ruleengine.engine.repository.VirtualSensorRedisRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VirtualSensorService {

    private final VirtualSensorRedisRepository virtualSensorRedisRepository;

    public VirtualSensorCreateResponse createVirtualSensor(
            Long organizationId,
            Long storageId,
            Long zoneId,
            VirtualSensorCreateRequest request
    ) {

        VirtualSensorConfig sensorConfig = VirtualSensorConfig.from(
                organizationId,
                storageId,
                zoneId,
                request
        );

        boolean saved = virtualSensorRedisRepository.saveIfAbsent(sensorConfig);

        if (!saved) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_EXISTS);
        }

        // Coordinator가 활성 목록을 확인한 뒤 Lock 소유 서버에서 Flow를 시작한다.
        virtualSensorRedisRepository.activate(zoneId);

        return VirtualSensorCreateResponse.from(zoneId, request.deviceEui());
    }

    public VirtualSensorUpdateResponse updateVirtualSensor(
            Long zoneId,
            VirtualSensorUpdateRequest request
    ) {
        VirtualSensorConfig existing = virtualSensorRedisRepository.getVirtualSensorConfig(zoneId)
                .orElseThrow(() -> new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND));

        VirtualSensorConfig sensorConfig = VirtualSensorConfig.merge(existing, request);

        boolean updated = virtualSensorRedisRepository.update(sensorConfig);

        if (!updated) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND);
        }

        return VirtualSensorUpdateResponse.from(updated);
    }

    public void deleteVirtualSensor(
            Long zoneId
    ) {
        virtualSensorRedisRepository.getVirtualSensorConfig(zoneId)
                .orElseThrow(() -> new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND));

        virtualSensorRedisRepository.delete(zoneId);
    }

    public VirtualSensorInfoResponse getVirtualSensor(
            Long zoneId
    ) {
        return virtualSensorRedisRepository.getVirtualSensorConfig(zoneId)
                .map(sensorConfig -> VirtualSensorInfoResponse.from(
                        sensorConfig,
                        virtualSensorRedisRepository.isActive(zoneId)
                ))
                .orElseGet(VirtualSensorInfoResponse::notRegistered);
    }


    public void changeFlowStatus(
            Long zoneId,
            VirtualSensorStatus status
    ) {
        //  상태변경시 검증?

        virtualSensorRedisRepository.getVirtualSensorConfig(zoneId)
                .orElseThrow(() -> new VirtualSensorFlowException(
                        ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND
                ));

        if (status == VirtualSensorStatus.ACTIVE) {
            virtualSensorRedisRepository.activate(zoneId);
            return;
        }

        if (status == VirtualSensorStatus.INACTIVE) {
            virtualSensorRedisRepository.deactivate(zoneId);
        }
    }
}
