package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class SensorInfluxService {

    private static final Pattern AGGREGATION_WINDOW_PATTERN =
            Pattern.compile("^[1-9]\\d{0,3}[smhd]$");

    private static final Duration DEFAULT_HISTORY_PERIOD =
            Duration.ofHours(24);

    private static final String DEFAULT_AGGREGATION_WINDOW = "10m";

    private final SensorInfluxRepository sensorInfluxRepository;

    public void save(SensorPayload sensorPayload) {

        if (sensorPayload == null) {
            throw new SensorDataException(ErrorCode.INVALID_SENSOR_DATA);
        }

        validateSectionIds(
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sensorPayload.sectionId()
        );

        SensorType sensorType =
                parseSensorType(sensorPayload.sensorType());

        Instant timestamp =
                parseTimestampOrNow(sensorPayload.time());

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

    public List<SensorPayload> findLatestBySection(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        validateSectionIds(organizationId, storageId, sectionId);

        return sensorInfluxRepository.findLatestBySection(
                organizationId,
                storageId,
                sectionId
        );
    }

    public List<SensorPayload> findLatestByStorage(
            Long organizationId,
            Long storageId
    ) {
        validateStorageIds(organizationId, storageId);

        return sensorInfluxRepository.findLatestByStorage(
                organizationId,
                storageId
        );
    }

    public List<SensorPayload> findLatestByOrganization(
            Long organizationId,
            String sensorType
    ) {
        validatePositiveId(organizationId, "organizationId");

        if (sensorType == null || sensorType.isBlank()) {
            return sensorInfluxRepository.findLatestByOrganization(
                    organizationId
            );
        }

        String resolvedSensorType =
                parseSensorType(sensorType).value();

        return sensorInfluxRepository
                .findLatestByOrganizationAndSensorType(
                        organizationId,
                        resolvedSensorType
                );
    }

    public List<SensorHistoryResponse> findHistoryBySection(
            Long organizationId,
            Long storageId,
            Long sectionId,
            String sensorType,
            Instant from,
            Instant to,
            String window
    ) {
        validateSectionIds(organizationId, storageId, sectionId);

        Instant resolvedTo = resolveTo(to);
        Instant resolvedFrom = resolveFrom(from, resolvedTo);
        String resolvedWindow = resolveWindow(window);
        String resolvedSensorType =
                parseOptionalSensorType(sensorType);

        validateTimeRange(resolvedFrom, resolvedTo);

        return sensorInfluxRepository.findHistoryBySection(
                organizationId,
                storageId,
                sectionId,
                resolvedSensorType,
                resolvedFrom,
                resolvedTo,
                resolvedWindow
        );
    }

    private Instant resolveTo(Instant to) {
        return to != null ? to : Instant.now();
    }

    private Instant resolveFrom(
            Instant from,
            Instant resolvedTo
    ) {
        return from != null
                ? from
                : resolvedTo.minus(DEFAULT_HISTORY_PERIOD);
    }

    private String resolveWindow(String window) {
        if (window == null || window.isBlank()) {
            return DEFAULT_AGGREGATION_WINDOW;
        }

        return normalizeWindow(window);
    }

    private String parseOptionalSensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            return null;
        }

        return parseSensorType(sensorType).value();
    }

    private SensorType parseSensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            throw new SensorDataException(
                    ErrorCode.INVALID_SENSOR_TYPE
            );
        }

        String normalizedSensorType =
                sensorType.trim().toLowerCase(Locale.ROOT);

        return SensorType.findByValue(normalizedSensorType)
                .orElseThrow(() -> new SensorDataException(
                        ErrorCode.UNSUPPORTED_SENSOR_TYPE
                ));
    }

    private void validateSectionIds(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        validateStorageIds(organizationId, storageId);
        validatePositiveId(sectionId, "sectionId");
    }

    private void validateStorageIds(
            Long organizationId,
            Long storageId
    ) {
        validatePositiveId(organizationId, "organizationId");
        validatePositiveId(storageId, "storageId");
    }

    private void validatePositiveId(
            Long id,
            String fieldName
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + "는 1 이상의 값이어야 합니다."
            );
        }
    }

    private void validateTimeRange(
            Instant from,
            Instant to
    ) {
        if (!from.isBefore(to)) {
            throw new IllegalArgumentException(
                    "조회 시작 시간은 종료 시간보다 이전이어야 합니다."
            );
        }
    }

    private String normalizeWindow(String window) {
        String normalizedWindow =
                window.trim().toLowerCase(Locale.ROOT);

        if (!AGGREGATION_WINDOW_PATTERN
                .matcher(normalizedWindow)
                .matches()) {
            throw new IllegalArgumentException(
                    "집계 주기는 1s, 5m, 1h, 1d 형식이어야 합니다."
            );
        }

        return normalizedWindow;
    }

    private Instant parseTimestampOrNow(String time) {
        if (time == null || time.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(time.trim());
        } catch (DateTimeParseException exception) {
            log.warn(
                    "센서 측정 시간 형식이 올바르지 않아 현재 시간으로 저장합니다. time={}",
                    time
            );

            return Instant.now();
        }
    }
}