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
@RequestMapping("/api/rule-engines")
@RequiredArgsConstructor
public class SensorQueryController {

    private final SensorInfluxService sensorInfluxService;

    @GetMapping("/latest/{locationId}")
    public ApiResponse<List<SensorPayloadDto>> findLatest(
            @PathVariable Long locationId
    ) {
        return ApiResponse.success(
                sensorInfluxService.findLatestByLocationId(locationId)
        );
    }
}
