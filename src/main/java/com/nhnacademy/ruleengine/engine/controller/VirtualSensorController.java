package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorStatusRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorUpdateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorCreateResponse;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorInfoResponse;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorUpdateResponse;
import com.nhnacademy.ruleengine.engine.service.OrganizationAccessService;
import com.nhnacademy.ruleengine.engine.service.VirtualSensorService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import com.nhnacademy.ruleengine.global.security.AccountUUID;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/*
    가상 센서는 조직에 속한다. 어느 구역에서 측정되는지는 여기서 정하지 않고,
    만들어진 deviceEui를 구역 센서로 등록하는 순간 정해진다.
 */
@RestController
@RequestMapping("/api/rule-engine/organizations/{organization-id}/virtual-sensors")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;
    private final OrganizationAccessService organizationAccessService;

    @PostMapping
    public ApiResponse<VirtualSensorCreateResponse> createVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @Valid @RequestBody VirtualSensorCreateRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        VirtualSensorCreateResponse response = virtualSensorService.createVirtualSensor(
                organizationId,
                request
        );

        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<VirtualSensorInfoResponse>> getVirtualSensors(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        return ApiResponse.success(virtualSensorService.getVirtualSensors(organizationId));
    }

    @GetMapping("/{device-eui}")
    public ApiResponse<VirtualSensorInfoResponse> getVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "device-eui") String deviceEui
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        VirtualSensorInfoResponse response =
                virtualSensorService.getVirtualSensor(organizationId, deviceEui);

        return ApiResponse.success(response);
    }

    @PutMapping("/{device-eui}")
    public ApiResponse<VirtualSensorUpdateResponse> updateVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "device-eui") String deviceEui,
            @Valid @RequestBody VirtualSensorUpdateRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        VirtualSensorUpdateResponse response = virtualSensorService.updateVirtualSensor(
                organizationId,
                deviceEui,
                request
        );

        return ApiResponse.success(response);
    }

    @DeleteMapping("/{device-eui}")
    public ApiResponse<Void> deleteVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "device-eui") String deviceEui
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        virtualSensorService.deleteVirtualSensor(organizationId, deviceEui);

        return ApiResponse.successNodata();
    }

    @PutMapping("/{device-eui}/status")
    public ApiResponse<Void> changeVirtualSensorStatus(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "device-eui") String deviceEui,
            @Valid @RequestBody VirtualSensorStatusRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        virtualSensorService.changeFlowStatus(
                organizationId,
                deviceEui,
                request.status()
        );

        return ApiResponse.successNodata();
    }
}
