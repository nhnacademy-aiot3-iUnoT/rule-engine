package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.LocationCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.SensorStatusRequest;
import com.nhnacademy.ruleengine.engine.service.VirtualSensorService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rule-engine/organizations/{organizationId}/storages/{storageId}/sections")
@RequiredArgsConstructor
public class VirtualSensorController {

    private final VirtualSensorService virtualSensorService;

    @PostMapping("/{sectionId}/virtual-sensor")
    public ApiResponse<Void> createVirtualSensor(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId,
            @Valid @RequestBody LocationCreateRequest request
    ) {
        virtualSensorService.createAndStart(
                organizationId,
                storageId,
                sectionId,
                request
        );

        return ApiResponse.successNodata();
    }

    @PatchMapping("/{sectionId}/status")
    public ApiResponse<Void> changeSectionStatus(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId,
            @Valid @RequestBody SensorStatusRequest request
    ) {
        virtualSensorService.changeStatus(
                organizationId,
                storageId,
                sectionId,
                request.status()
        );

        return ApiResponse.successNodata();
    }
}