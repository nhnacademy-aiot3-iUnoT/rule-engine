package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;

import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
// 센서 데이터 저장 요청을 Repository로 전달한다.
public class SensorInfluxService {

    private final SensorInfluxRepository sensorInfluxRepository;

    public void save(SensorPayloadDto sensorPayload) {
        sensorInfluxRepository.save(
                sensorPayload.locationId(),
                sensorPayload.applicationName(),
                sensorPayload.location(),
                sensorPayload.sensorType(),
                sensorPayload.value(),
                sensorPayload.unit(),
                sensorPayload.deviceName(),
                sensorPayload.deviceEui(),
                parseTimestamp(sensorPayload.time())
        );
    }


    public List<SensorPayloadDto> findLatestByLocationId(Long locationId) {
        if (locationId == null || locationId <= 0) {
            throw new IllegalArgumentException("locationId는 양수여야 합니다.");
        }

        return sensorInfluxRepository.findLatestByLocationId(locationId);
    }

    public List<SensorPayloadDto> findLatestBySensorType(String sensorType) {
        validateSensorType(sensorType);

        return sensorInfluxRepository.findLatestBySensorType(
                sensorType.toLowerCase()
        );
    }

    private void validateSensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            throw new IllegalArgumentException(
                    "sensorType은 필수입니다."
            );
        }

        String normalizedSensorType = sensorType.toLowerCase();

        if (!List.of("temperature", "humidity", "door")
                .contains(normalizedSensorType)) {
            throw new IllegalArgumentException(
                    "지원하지 않는 센서 종류입니다: " + sensorType
            );
        }
    }

    private Instant parseTimestamp(String time) {
        if (time == null || time.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(time);
        } catch (RuntimeException ignored) {
            return Instant.now();
        }
    }
}
