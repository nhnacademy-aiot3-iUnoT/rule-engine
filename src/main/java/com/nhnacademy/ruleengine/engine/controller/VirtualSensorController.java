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

import java.util.UUID;

@RestController
@RequestMapping("/api/rule-engine/organizations/{organizationId}/storages/{storageId}/zones")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;
    private final OrganizationAccessService organizationAccessService;

    @PostMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<VirtualSensorCreateResponse> createVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId,
            @Valid @RequestBody VirtualSensorCreateRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        VirtualSensorCreateResponse response = virtualSensorService.createVirtualSensor(
                organizationId,
                storageId,
                zoneId,
                request
        );

        return ApiResponse.success(response);
    }


    @PutMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<VirtualSensorUpdateResponse> updateVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId,
            @Valid @RequestBody VirtualSensorUpdateRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        VirtualSensorUpdateResponse response = virtualSensorService.updateVirtualSensor(organizationId, storageId, zoneId, request);
        return ApiResponse.success(response);

    }

    @DeleteMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<Void> deleteVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        virtualSensorService.deleteVirtualSensor(organizationId, storageId, zoneId);
        return ApiResponse.successNodata();
    }

    @GetMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<VirtualSensorInfoResponse> getVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        VirtualSensorInfoResponse response = virtualSensorService.getVirtualSensor(organizationId, storageId, zoneId);
        return ApiResponse.success(response);
    }


    @PutMapping("/{zoneId}/status")
    public ApiResponse<Void> changeVirtualSensorStatus(
            @AccountUUID UUID accountUuid,
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId,
            @Valid @RequestBody VirtualSensorStatusRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        virtualSensorService.changeFlowStatus(
                organizationId,
                storageId,
                zoneId,
                request.status()
        );

        return ApiResponse.successNodata();
    }
}
