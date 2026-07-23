package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.SensorType;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SensorInfluxService {

    private final SensorInfluxRepository sensorInfluxRepository;

    // 룰엔진 내부에서 전달받은 센서 데이터를 InfluxDB에 저장한다.
    public void save(SensorPayloadDto sensorPayload) {
        Instant timestamp = parseTimestampOrNow(sensorPayload.time());
        SensorType sensorType = parseSensorType(sensorPayload.sensorType());

        sensorInfluxRepository.save(
                sensorPayload.organizationId(),
                sensorPayload.deviceEui(),
                sensorPayload.storageId(),
                sensorPayload.sectionId(),
                sensorType.value(),
                sensorPayload.value(),
                sensorPayload.unit(),
                timestamp
        );
    }

    // 조직의 창고에 있는 구역의 센서 종류별 최신 데이터를 조회한다.
    public List<SensorPayloadDto> findLatestBySectionId(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        validateSectionHierarchy(organizationId, storageId, sectionId);

        return sensorInfluxRepository.findLatestBySectionId(organizationId, storageId, sectionId);
    }

    // 조직의 전체 창고의 특정 센서 타입의 구역별 최신 데이터를 조회한다.
    public List<SensorPayloadDto> findLatestBySensorType(
            Long organizationId,
            String sensorType
    ) {
        validateOrganizationId(organizationId);
        SensorType parsedSensorType = parseSensorType(sensorType);

        return sensorInfluxRepository.findLatestOrganizationBySensorType(organizationId, parsedSensorType.value());
    }

    private void validateSectionHierarchy(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        // Todo section 유효한지 검증 로직
    }

    private void validateOrganizationId(Long organizationId) {
        // Todo organization 유효한지 검증로직
    }

    private static SensorType parseSensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            throw new SensorDataException(ErrorCode.INVALID_SENSOR_TYPE);
        }

        return SensorType.findByValue(sensorType)
                .orElseThrow(() -> new SensorDataException(
                        ErrorCode.UNSUPPORTED_SENSOR_TYPE
                ));
    }

    private static Instant parseTimestampOrNow(String time) {
        if (time == null || time.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(time);
        } catch (DateTimeParseException ignored) {
            return Instant.now();
        }
    }
}
