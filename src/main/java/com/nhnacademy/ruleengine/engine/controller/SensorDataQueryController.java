package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryQueryRequest;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorLatestResponse;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine")
public class SensorDataQueryController {

    private final SensorInfluxService sensorInfluxService;

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
            @PathVariable Long organizationId,
            @RequestParam(required = false) String sensorType
    ) {
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
