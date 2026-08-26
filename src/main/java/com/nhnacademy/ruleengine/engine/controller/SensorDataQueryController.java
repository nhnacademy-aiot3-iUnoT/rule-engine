package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryQueryRequest;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorLatestResponse;
import com.nhnacademy.ruleengine.engine.service.OrganizationAccessService;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import com.nhnacademy.ruleengine.global.security.AccountUUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine")
public class SensorDataQueryController {

    private final SensorInfluxService sensorInfluxService;
    private final OrganizationAccessService organizationAccessService;

    @GetMapping("/zones/{zoneId}/sensor-data/latest")
    public ApiResponse<List<SensorLatestResponse>> findLatestByZone(
            @PathVariable Long zoneId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByZone(zoneId)
                        .stream()
                        .map(SensorLatestResponse::from)
                        .toList()
        );
    }

    @GetMapping("/zones/{zoneId}/sensor-data/history")
    public ApiResponse<List<SensorHistoryResponse>> findHistoryByZone(
            @PathVariable Long zoneId,
            @ModelAttribute SensorHistoryQueryRequest request
    ) {
        return ApiResponse.success(
                sensorInfluxService.findHistoryByZone(
                        zoneId,
                        request.sensorType(),
                        request.from(),
                        request.to(),
                        request.window()
                )
        );
    }

    @GetMapping("/storages/{storageId}/sensor-data/latest")
    public ApiResponse<List<SensorLatestResponse>> findLatestByStorage(
            @PathVariable Long storageId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByStorage(storageId)
                        .stream()
                        .map(SensorLatestResponse::from)
                        .toList()
        );
    }

    @GetMapping("/organizations/{organizationId}/sensor-data/latest")
    public ApiResponse<List<SensorLatestResponse>> findLatestByOrganization(
            @AccountUUID UUID accountUuid,
            @PathVariable Long organizationId,
            @RequestParam(required = false) String sensorType
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        return ApiResponse.success(
                sensorInfluxService.findLatestByOrganization(
                        organizationId,
                        sensorType
                )
                        .stream()
                        .map(SensorLatestResponse::from)
                        .toList()
        );
    }
}
