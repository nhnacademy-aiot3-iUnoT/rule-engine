package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatusRequest;
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
            @Valid @RequestBody VirtualSensorCreateRequest request
    ) {
        virtualSensorService.createAndStartFlow(
                organizationId,
                storageId,
                sectionId,
                request
        );

        return ApiResponse.successNodata();
    }

    @PatchMapping("/{sectionId}/status")
    public ApiResponse<Void> changeVirtualSensorStatus(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId,
            @Valid @RequestBody VirtualSensorStatusRequest request
    ) {
        virtualSensorService.changeFlowStatus(
                organizationId,
                storageId,
                sectionId,
                request.status()
        );

        return ApiResponse.successNodata();
    }
}
