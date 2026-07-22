package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.LocationCreateRequest;
import com.nhnacademy.ruleengine.engine.service.VirtualSensorService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rule-engine/organizations")
@RequiredArgsConstructor
public class CustomSensorController {

    private final VirtualSensorService virtualSensorService;

    @PostMapping(
            "/{organizationId}/storages/{storageId}"
                    + "/sections/{sectionId}/virtual-sensor"
    )
    public ApiResponse<Void> createSection(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId,
            @Valid @RequestBody LocationCreateRequest request

    ) {

        virtualSensorService.createAndStart(organizationId, storageId, sectionId,request);
        return ApiResponse.successNodata();

    }

}
