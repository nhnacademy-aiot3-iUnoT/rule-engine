package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.dto.SensorType;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.repository.SensorInfluxRepository;
import com.nhnacademy.ruleengine.engine.validation.LocationHierarchyValidator;
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
    private static final Pattern HISTORY_SENSOR_TYPE_PATTERN =
            Pattern.compile("^[a-z][a-z0-9_-]{0,63}$");
    private static final Duration DEFAULT_HISTORY_PERIOD = Duration.ofHours(24);
    private static final String DEFAULT_AGGREGATION_WINDOW = "10m";

    private final SensorInfluxRepository sensorInfluxRepository;
    private final LocationHierarchyValidator locationHierarchyValidator;

    /**
     * 센서 데이터를 InfluxDB에 저장한다.
     */
    public void save(SensorPayloadDto sensorPayload) {
        SensorType sensorType = parseSensorType(sensorPayload.sensorType());
        Instant timestamp = parseTimestampOrNow(sensorPayload.time());

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

    /**
     * 특정 구역의 센서별 최신 데이터를 조회한다.
     */
    public List<SensorPayloadDto> findLatestBySectionId(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        validateSectionHierarchy(organizationId, storageId, sectionId);

        return sensorInfluxRepository.findLatestBySectionId(
                organizationId,
                storageId,
                sectionId
        );
    }

    /**
     * 특정 창고에 속한 모든 구역의 센서별 최신 데이터를 조회한다.
     */
    public List<SensorPayloadDto> findLatestByStorageId(
            Long organizationId,
            Long storageId
    ) {
        locationHierarchyValidator.validateStorage(
                organizationId,
                storageId
        );

        return sensorInfluxRepository.findLatestByStorageId(
                organizationId,
                storageId
        );
    }

    /**
     * 조직의 최신 센서 데이터를 조회한다.
     *
     * sensorType이 없으면 모든 센서 타입을 조회한다.
     */
    public List<SensorPayloadDto> findLatestByOrganization(
            Long organizationId,
            String sensorType
    ) {
        locationHierarchyValidator.validateOrganization(organizationId);

        if (sensorType == null) {
            return sensorInfluxRepository.findLatestByOrganizationId(
                    organizationId
            );
        }

        SensorType parsedSensorType = parseSensorType(sensorType);

        return sensorInfluxRepository.findLatestByOrganizationAndSensorType(
                organizationId,
                parsedSensorType.value()
        );
    }

    /**
     * 특정 구역의 센서 이력을 기간과 집계 간격에 따라 조회한다.
     *
     * sensorType이 없으면 구역의 모든 센서 타입을 조회한다.
     */
    public List<SensorHistoryResponse> findHistory(
            Long organizationId,
            Long storageId,
            Long sectionId,
            String sensorType,
            Instant from,
            Instant to,
            String window
    ) {
        validateSectionHierarchy(organizationId, storageId, sectionId);

        Instant resolvedTo = to != null ? to : Instant.now();
        Instant resolvedFrom = from != null
                ? from
                : resolvedTo.minus(DEFAULT_HISTORY_PERIOD);
        String resolvedWindow = window == null || window.isBlank()
                ? DEFAULT_AGGREGATION_WINDOW
                : window;

        validateTimeRange(resolvedFrom, resolvedTo);
        String normalizedSensorType =
                normalizeOptionalHistorySensorType(sensorType);
        String normalizedWindow = normalizeWindow(resolvedWindow);

        return sensorInfluxRepository.findHistory(
                organizationId,
                storageId,
                sectionId,
                normalizedSensorType,
                resolvedFrom,
                resolvedTo,
                normalizedWindow
        );
    }

    private void validateSectionHierarchy(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        locationHierarchyValidator.validateSection(
                organizationId,
                storageId,
                sectionId
        );
    }

    /**
     * 필수 센서 타입을 검증하고 SensorType으로 변환한다.
     */
    private SensorType parseSensorType(String sensorType) {
        String normalizedSensorType = normalizeRequiredText(
                sensorType,
                ErrorCode.INVALID_SENSOR_TYPE
        );

        return SensorType.findByValue(normalizedSensorType)
                .orElseThrow(() -> new SensorDataException(
                        ErrorCode.UNSUPPORTED_SENSOR_TYPE
                ));
    }

    private String normalizeOptionalHistorySensorType(String sensorType) {
        if (sensorType == null || sensorType.isBlank()) {
            return null;
        }

        String normalizedSensorType = sensorType
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!HISTORY_SENSOR_TYPE_PATTERN
                .matcher(normalizedSensorType)
                .matches()) {
            throw new IllegalArgumentException(
                    "sensorType은 영문, 숫자, 밑줄, 하이픈만 사용할 수 있습니다."
            );
        }

        return normalizedSensorType;
    }

    private String normalizeRequiredText(
            String value,
            ErrorCode emptyValueErrorCode
    ) {
        if (value == null || value.isBlank()) {
            throw new SensorDataException(emptyValueErrorCode);
        }

        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void validateTimeRange(Instant from, Instant to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "조회 시작 시간과 종료 시간은 필수입니다."
            );
        }

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
