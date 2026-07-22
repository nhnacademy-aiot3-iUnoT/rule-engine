package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.location.SectionCatalog;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SensorInfluxService {

    private static final Set<String> SUPPORTED_SENSOR_TYPES = Set.of(
            "temperature",
            "humidity",
            "door"
    );

    private final SensorInfluxRepository sensorInfluxRepository;
    private final SectionCatalog sectionCatalog;

    // 룰엔진 내부에서 전달받은 센서 데이터를 InfluxDB에 저장한다.
    public void save(SensorPayloadDto sensorPayload) {
        sensorInfluxRepository.save(
                sensorPayload.organizationId(),
                sensorPayload.deviceEui(),
                sensorPayload.storageId(),
                sensorPayload.sectionId(),
                sensorPayload.sensorType(),
                sensorPayload.value(),
                sensorPayload.unit(),
                parseTimestamp(sensorPayload.time())
        );
    }

    // 특정 위치의 센서 종류별 최신 데이터를 조회한다.
    public List<SensorPayloadDto> findLatestByStorageId(Long storageId) {
        validateStorageId(storageId);

        return sensorInfluxRepository.findLatestByStorageId(storageId);
    }

    // 특정 센서 타입의 위치별 최신 데이터를 조회한다.
    public List<SensorPayloadDto> findLatestBySensorType(String sensorType) {
        String normalizedSensorType = normalizeAndValidateSensorType(sensorType);

        return sensorInfluxRepository.findLatestBySensorType(
                normalizedSensorType
        );
    }

    private void validateStorageId(Long storageId) {
        if (storageId == null || storageId <= 0) {
            throw new SensorDataException(
                    ErrorCode.INVALID_STORAGE_ID
            );
        }

        if (!sectionCatalog.exists(storageId)) {
            throw new SensorDataException(
                    ErrorCode.STORAGE_NOT_FOUND
            );
        }
    }

    private String normalizeAndValidateSensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            throw new SensorDataException(
                    ErrorCode.INVALID_SENSOR_TYPE
            );
        }

        String normalizedSensorType = sensorType
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!SUPPORTED_SENSOR_TYPES.contains(normalizedSensorType)) {
            throw new SensorDataException(
                    ErrorCode.UNSUPPORTED_SENSOR_TYPE
            );
        }

        return normalizedSensorType;
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
