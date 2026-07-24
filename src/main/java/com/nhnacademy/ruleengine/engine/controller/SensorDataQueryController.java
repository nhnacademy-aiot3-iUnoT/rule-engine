package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryQueryRequest;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
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

    @GetMapping("/sections/{sectionId}/sensor-data/latest")
    public ApiResponse<List<SensorPayload>> findLatestBySection(
            @PathVariable Long sectionId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestBySection(sectionId)
        );
    }

    @GetMapping("/sections/{sectionId}/sensor-data/history")
    public ApiResponse<List<SensorHistoryResponse>> findHistoryBySection(
            @PathVariable Long sectionId,
            @ModelAttribute SensorHistoryQueryRequest request
    ) {
        return ApiResponse.success(
                sensorInfluxService.findHistoryBySection(
                        sectionId,
                        request.sensorType(),
                        request.from(),
                        request.to(),
                        request.window()
                )
        );
    }

    @GetMapping("/storages/{storageId}/sensor-data/latest")
    public ApiResponse<List<SensorPayload>> findLatestByStorage(
            @PathVariable Long storageId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByStorage(storageId)
        );
    }

    @GetMapping("/organizations/{organizationId}/sensor-data/latest")
    public ApiResponse<List<SensorPayload>> findLatestByOrganization(
            @PathVariable Long organizationId,
            @RequestParam(required = false) String sensorType
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByOrganization(
                        organizationId,
                        sensorType
                )
        );
    }
}