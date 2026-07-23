package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryQueryRequest;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
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

    @GetMapping(
            "/organizations/{organizationId}/storages/{storageId}"
                    + "/sections/{sectionId}/sensor-data/latest"
    )
    public ApiResponse<List<SensorPayload>> findLatestBySection(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestBySection(
                        organizationId,
                        storageId,
                        sectionId
                )
        );
    }

    @GetMapping(
            "/organizations/{organizationId}/sensor-data/latest"
    )
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

    @GetMapping(
            "/organizations/{organizationId}/storages/{storageId}/sensor-data/latest"
    )
    public ApiResponse<List<SensorPayload>> findLatestByStorage(
            @PathVariable Long organizationId,
            @PathVariable Long storageId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByStorage(
                        organizationId,
                        storageId
                )
        );
    }

    @GetMapping(
            "/organizations/{organizationId}/storages/{storageId}"
                    + "/sections/{sectionId}/sensor-data/history"
    )
    public ApiResponse<List<SensorHistoryResponse>> findHistoryBySection(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId,
            @ModelAttribute SensorHistoryQueryRequest request
    ) {
        return ApiResponse.success(
                sensorInfluxService.findHistoryBySection(
                        organizationId,
                        storageId,
                        sectionId,
                        request.sensorType(),
                        request.from(),
                        request.to(),
                        request.window()
                )
        );
    }
}
