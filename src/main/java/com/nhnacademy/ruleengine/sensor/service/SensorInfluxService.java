package com.nhnacademy.ruleengine.sensor.service;

import com.nhnacademy.ruleengine.sensor.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.sensor.dto.RoomLatestSensorResponse;
import com.nhnacademy.ruleengine.sensor.dto.RoomLatestSensorResponse.SensorValueResponse;
import com.nhnacademy.ruleengine.sensor.repository.SensorInfluxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
// 센서 데이터 저장 요청을 Repository로 전달한다.
public class SensorInfluxService {

    private final SensorInfluxRepository sensorInfluxRepository;

    public void save(SensorPayloadDto sensorPayload) {
        sensorInfluxRepository.save(
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

    public RoomLatestSensorResponse getLatestSensorData(String location) {
        String normalizedLocation = requireLocation(location);
        Map<String, SensorValueResponse> sensors = new LinkedHashMap<>();

        sensorInfluxRepository.findLatestByLocation(normalizedLocation)
                .forEach(reading -> sensors.put(
                        reading.sensorType(),
                        new SensorValueResponse(
                                reading.value(),
                                reading.unit(),
                                reading.deviceName(),
                                reading.deviceEui(),
                                reading.measuredAt()
                        )
                ));

        return new RoomLatestSensorResponse(
                normalizedLocation,
                Map.copyOf(sensors)
        );
    }

    private String requireLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("location은 비어 있을 수 없습니다.");
        }
        return location.trim();
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
