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
@RequestMapping("/api/rule-engine/organizations/{organization-id}")
public class SensorDataQueryController {

    private final SensorInfluxService sensorInfluxService;
    private final OrganizationAccessService organizationAccessService;

    @GetMapping("/zones/{zone-id}/sensor-data/latest")
    public ApiResponse<List<SensorLatestResponse>> findLatestByZone(
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "zone-id") Long zoneId,
            @AccountUUID UUID accountUuid
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        return ApiResponse.success(
                sensorInfluxService.findLatestByZone(zoneId)
                        .stream()
                        .map(SensorLatestResponse::from)
                        .toList()
        );
    }

    @GetMapping("/zones/{zone-id}/sensor-data/history")
    public ApiResponse<List<SensorHistoryResponse>> findHistoryByZone(
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "zone-id") Long zoneId,
            @AccountUUID UUID accountUuid,
            @ModelAttribute SensorHistoryQueryRequest request
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

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

    @GetMapping("/storages/{storage-id}/sensor-data/latest")
    public ApiResponse<List<SensorLatestResponse>> findLatestByStorage(
            @PathVariable(name = "organization-id") Long organizationId,
            @PathVariable(name = "storage-id") Long storageId,
            @AccountUUID UUID accountUuid
    ) {
        organizationAccessService.verifyOrganization(accountUuid, organizationId);

        return ApiResponse.success(
                sensorInfluxService.findLatestByStorage(storageId)
                        .stream()
                        .map(SensorLatestResponse::from)
                        .toList()
        );
    }

    @GetMapping("/sensor-data/latest")
    public ApiResponse<List<SensorLatestResponse>> findLatestByOrganization(
            @AccountUUID UUID accountUuid,
            @PathVariable(name = "organization-id") Long organizationId,
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
