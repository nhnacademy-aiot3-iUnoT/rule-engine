package com.nhnacademy.ruleengine.engine.controller;

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
    public ApiResponse<List<SensorPayloadDto>> findLatestBySensorType(
            @PathVariable Long organizationId,
            @RequestParam(required = false) String sensorType
    ) {
        // 타입이 없는경우 모든 센서데이터 조회
        if(sensorType == null) {
            return ApiResponse.success(
                    sensorInfluxService.findLatestByOrganizationId(organizationId)
            );
        }

        return ApiResponse.success(
                sensorInfluxService.findLatestBySensorType(
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
}