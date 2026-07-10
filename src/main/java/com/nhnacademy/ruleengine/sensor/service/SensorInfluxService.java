package com.nhnacademy.ruleengine.sensor.service;

import com.nhnacademy.ruleengine.sensor.repository.SensorInfluxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
// 센서 데이터 저장 요청을 Repository로 전달한다.
public class SensorInfluxService {

    private final SensorInfluxRepository sensorInfluxRepository;

    public void save(
            String storage,
            String sensorType,
            double value,
            String unit,
            String deviceName,
            String deviceEui
    ) {
        sensorInfluxRepository.save(
                storage,
                sensorType,
                value,
                unit,
                deviceName,
                deviceEui
        );
    }
}
