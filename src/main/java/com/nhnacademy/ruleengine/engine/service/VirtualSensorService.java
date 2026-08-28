package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 가상 센서는 구역이 아니라 조직에 속한다. 어느 구역에서 측정되는지는 deviceEui를 구역 센서로
 * 등록하는 순간 정해지며, 이 서비스는 그 등록 여부를 조회해서 화면에 보여주기만 한다.
 */
@Service
@RequiredArgsConstructor
public class VirtualSensorService {

    private final VirtualSensorRedisRepository virtualSensorRedisRepository;
    private final ZoneResolver zoneResolver;

    public VirtualSensorCreateResponse createVirtualSensor(
            Long organizationId,
            VirtualSensorCreateRequest request
    ) {
        VirtualSensorConfig sensorConfig = VirtualSensorConfig.from(organizationId, request);

        boolean saved = virtualSensorRedisRepository.saveIfAbsent(sensorConfig);

        if (!saved) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_EXISTS);
        }

        // Coordinator가 활성 목록을 확인한 뒤 Lock 소유 서버에서 Flow를 시작한다.
        // 구역에 등록되기 전까지는 만들어진 데이터가 버려질 뿐, 생성 자체는 바로 돌아간다.
        virtualSensorRedisRepository.activate(sensorConfig.deviceEui());

        return VirtualSensorCreateResponse.from(sensorConfig.deviceEui());
    }

    public VirtualSensorUpdateResponse updateVirtualSensor(
            Long organizationId,
            String deviceEui,
            VirtualSensorUpdateRequest request
    ) {
        VirtualSensorConfig existing = getOwnedConfig(organizationId, deviceEui);

        VirtualSensorConfig sensorConfig = VirtualSensorConfig.merge(existing, request);

        boolean updated = virtualSensorRedisRepository.update(sensorConfig);

        if (!updated) {
            throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND);
        }

        return VirtualSensorUpdateResponse.from(updated);
    }

    public void deleteVirtualSensor(
            Long organizationId,
            String deviceEui
    ) {
        getOwnedConfig(organizationId, deviceEui);

        virtualSensorRedisRepository.delete(organizationId, deviceEui);
    }

    public VirtualSensorInfoResponse getVirtualSensor(
            Long organizationId,
            String deviceEui
    ) {
        return findOwnedConfig(organizationId, deviceEui)
                .map(this::toInfoResponse)
                .orElseGet(VirtualSensorInfoResponse::notRegistered);
    }

    // 조직의 가상 센서 목록. 관리 화면이 보여줄 목록이라 deviceEui 순으로 정렬해 돌려준다.
    public List<VirtualSensorInfoResponse> getVirtualSensors(Long organizationId) {
        return virtualSensorRedisRepository.findDeviceEuisByOrganization(organizationId)
                .stream()
                .map(virtualSensorRedisRepository::getVirtualSensorConfig)
                .flatMap(Optional::stream)
                .filter(config -> config.ownedBy(organizationId))
                .map(this::toInfoResponse)
                .sorted(Comparator.comparing(VirtualSensorInfoResponse::deviceEui))
                .toList();
    }

    public void changeFlowStatus(
            Long organizationId,
            String deviceEui,
            VirtualSensorStatus status
    ) {
        getOwnedConfig(organizationId, deviceEui);

        if (status == VirtualSensorStatus.ACTIVE) {
            if (!registeredZoneActive(deviceEui)) {
                throw new VirtualSensorFlowException(ErrorCode.VIRTUAL_SENSOR_ZONE_INACTIVE);
            }

            virtualSensorRedisRepository.activate(deviceEui);
            return;
        }

        if (status == VirtualSensorStatus.INACTIVE) {
            virtualSensorRedisRepository.deactivate(deviceEui);
        }
    }

    private VirtualSensorInfoResponse toInfoResponse(VirtualSensorConfig config) {
        // 아직 구역에 등록하지 않았으면 데이터가 버려지고 있다는 뜻이라, 화면에서 구분해 보여준다.
        Long registeredZoneId = zoneResolver.resolve(config.deviceEui())
                .map(ResolvedZoneResponse::zoneId)
                .orElse(null);

        // 구역이나 저장소가 비활성이면 Coordinator가 Flow를 내리므로, 화면에도 실제로 도는 상태를 보여준다.
        boolean active = virtualSensorRedisRepository.isActive(config.deviceEui())
                && (registeredZoneId == null || zoneResolver.isZoneActive(registeredZoneId));

        return VirtualSensorInfoResponse.from(
                config,
                active,
                registeredZoneId
        );
    }

    // 구역에 등록하지 않은 가상 센서는 비활성 구역에 묶여 있지 않으므로 활성화를 막지 않는다.
    private boolean registeredZoneActive(String deviceEui) {
        return zoneResolver.resolve(deviceEui)
                .map(zone -> zoneResolver.isZoneActive(zone.zoneId()))
                .orElse(true);
    }

    private VirtualSensorConfig getOwnedConfig(Long organizationId, String deviceEui) {
        return findOwnedConfig(organizationId, deviceEui)
                .orElseThrow(() -> new VirtualSensorFlowException(
                        ErrorCode.VIRTUAL_SENSOR_CONFIG_NOT_FOUND
                ));
    }

    // 다른 조직의 deviceEui는 없는 것과 똑같이 취급해, 존재 여부조차 알려주지 않는다.
    private Optional<VirtualSensorConfig> findOwnedConfig(Long organizationId, String deviceEui) {
        return virtualSensorRedisRepository.getVirtualSensorConfig(deviceEui)
                .filter(config -> config.ownedBy(organizationId));
    }
}
