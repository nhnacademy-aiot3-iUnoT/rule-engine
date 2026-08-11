package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.virtual.*;
import com.nhnacademy.ruleengine.engine.service.VirtualSensorService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rule-engine/organizations/{organizationId}/storages/{storageId}/zones")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;

    @PostMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<VirtualSensorCreateResponse> createVirtualSensor(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId,
            @Valid @RequestBody VirtualSensorCreateRequest request
    ) {
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
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId,
            @Valid @RequestBody VirtualSensorUpdateRequest request
    ) {
        VirtualSensorUpdateResponse response = virtualSensorService.updateVirtualSensor(organizationId, storageId, zoneId, request);
        return ApiResponse.success(response);

    }

    @DeleteMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<Void> deleteVirtualSensor(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId
    ) {
        virtualSensorService.deleteVirtualSensor(organizationId, storageId, zoneId);
        return ApiResponse.successNodata();
    }

    @GetMapping("/{zoneId}/virtual-sensor")
    public ApiResponse<VirtualSensorInfoResponse> getVirtualSensor(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId
    ) {
        VirtualSensorInfoResponse response = virtualSensorService.getVirtualSensor(organizationId, storageId, zoneId);
        return ApiResponse.success(response);
    }


    @PutMapping("/{zoneId}/status")
    public ApiResponse<Void> changeVirtualSensorStatus(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long zoneId,
            @Valid @RequestBody VirtualSensorStatusRequest request
    ) {
        virtualSensorService.changeFlowStatus(
                organizationId,
                storageId,
                zoneId,
                request.status()
        );

        return ApiResponse.successNodata();
    }
}
