package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.service.ZoneEnvStatusService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine/internal/zones")
public class EnvironmentStatusController {

    private final ZoneEnvStatusService zoneEnvStatusService;

    @PostMapping("/{zoneId}/env-status/resolve")
    public ApiResponse<Void> resolveEnvStatus(
            @RequestParam Long organizationId,
            @RequestParam Long storageId,
            @PathVariable Long zoneId
    ){
        zoneEnvStatusService.resolveCriticalStates(organizationId, storageId, zoneId);
        return ApiResponse.successNodata();
    }
}
