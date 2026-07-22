package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine/sensor-data")
public class SensorDataQueryController {

    private final SensorInfluxService sensorInfluxService;

    @GetMapping("/storages/{storageId}/latest")
    public ApiResponse<List<SensorPayloadDto>> findLatestByStorageId(
            @PathVariable Long storageId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByStorageId(storageId)
        );
    }

    @GetMapping("/types/{sensorType}/latest")
    public ApiResponse<List<SensorPayloadDto>> findLatestBySensorType(
            @PathVariable String sensorType
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestBySensorType(sensorType)
        );
    }
}