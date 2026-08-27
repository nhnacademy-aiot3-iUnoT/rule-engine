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
@RequestMapping("/api/rule-engine/organizations/{organization-id}/storages/{storage-id}/zones")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;
    private final OrganizationAccessService organizationAccessService;

    @PostMapping("/{zone-id}/virtual-sensor")
    public ApiResponse<VirtualSensorCreateResponse> createVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "storage-id") Long storageId,
            @PathVariable(name = "zone-id") Long zoneId,
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


    @PutMapping("/{zone-id}/virtual-sensor")
    public ApiResponse<VirtualSensorUpdateResponse> updateVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "storage-id") Long storageId,
            @PathVariable(name = "zone-id") Long zoneId,
            @Valid @RequestBody VirtualSensorUpdateRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        VirtualSensorUpdateResponse response = virtualSensorService.updateVirtualSensor(zoneId, request);
        return ApiResponse.success(response);

    }

    @DeleteMapping("/{zone-id}/virtual-sensor")
    public ApiResponse<Void> deleteVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "storage-id") Long storageId,
            @PathVariable(name = "zone-id") Long zoneId
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        virtualSensorService.deleteVirtualSensor(zoneId);
        return ApiResponse.successNodata();
    }

    @GetMapping("/{zone-id}/virtual-sensor")
    public ApiResponse<VirtualSensorInfoResponse> getVirtualSensor(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "storage-id") Long storageId,
            @PathVariable(name = "zone-id") Long zoneId
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        VirtualSensorInfoResponse response = virtualSensorService.getVirtualSensor(zoneId);
        return ApiResponse.success(response);
    }


    @PutMapping("/{zone-id}/status")
    public ApiResponse<Void> changeVirtualSensorStatus(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "storage-id") Long storageId,
            @PathVariable(name = "zone-id") Long zoneId,
            @Valid @RequestBody VirtualSensorStatusRequest request
    ) {
        organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId);

        virtualSensorService.changeFlowStatus(
                zoneId,
                request.status()
        );

        return ApiResponse.successNodata();
    }
}
