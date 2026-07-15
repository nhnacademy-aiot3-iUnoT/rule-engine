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

    @GetMapping("/locations/{locationId}/latest")
    public ApiResponse<List<SensorPayloadDto>> findLatestByLocationId(
            @PathVariable Long locationId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByLocationId(locationId)
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