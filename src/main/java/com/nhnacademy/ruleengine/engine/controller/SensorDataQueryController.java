package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.SensorHistoryQueryRequest;
import com.nhnacademy.ruleengine.engine.dto.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
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
    public ApiResponse<List<SensorPayloadDto>> findLatestBySectionId(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestBySectionId(
                        organizationId,
                        storageId,
                        sectionId
                )
        );
    }

    @GetMapping(
            "/organizations/{organizationId}/sensor-data/latest"
    )
    public ApiResponse<List<SensorPayloadDto>> findLatestByOrganization(
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
    public ApiResponse<List<SensorPayloadDto>> findLatestByStorageId(
            @PathVariable Long organizationId,
            @PathVariable Long storageId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByStorageId(
                        organizationId,
                        storageId
                )
        );
    }

    @GetMapping(
            "/organizations/{organizationId}/storages/{storageId}"
                    + "/sections/{sectionId}/sensor-data/history"
    )
    public ApiResponse<List<SensorHistoryResponse>> findHistory(
            @PathVariable Long organizationId,
            @PathVariable Long storageId,
            @PathVariable Long sectionId,
            @ModelAttribute SensorHistoryQueryRequest request
    ) {
        return ApiResponse.success(
                sensorInfluxService.findHistory(
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
