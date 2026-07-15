package com.nhnacademy.ruleengine.sensor.controller;

import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import com.nhnacademy.ruleengine.sensor.dto.RoomLatestSensorResponse;
import com.nhnacademy.ruleengine.sensor.service.SensorInfluxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rule-engine")
public class SensorQueryController {

    private final SensorInfluxService sensorInfluxService;

    @GetMapping("/latest")
    public ApiResponse<RoomLatestSensorResponse> getLatestSensorData(
            @RequestParam String location
    ) {
        return ApiResponse.success(
                sensorInfluxService.getLatestSensorData(location)
        );
    }
}