package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.validation.SensorPayloadValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnvironmentProcessingService {

    private final SensorInfluxService sensorInfluxService;

    public void process(SensorPayload sensorPayload) {
        SensorPayloadValidator.validate(sensorPayload);

        sensorInfluxService.save(sensorPayload);

        log.info(
                "환경 데이터 처리 완료. sectionId={}, sensorType={}",
                sensorPayload.sectionId(),
                sensorPayload.sensorType()
        );

        // TODO 룰 구현 추가예정
    }
}