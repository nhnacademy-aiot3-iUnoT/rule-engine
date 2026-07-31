package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorDataWriteCommand;
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

import static com.nhnacademy.ruleengine.engine.constants.LocationFields.*;

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

    /**
     * 센서 데이터를 InfluxDB에 저장한다.
     */
    public void save(SensorPayload sensorPayload) {
        if (sensorPayload == null) {
            throw new SensorDataException(
                    ErrorCode.INVALID_SENSOR_DATA
            );
        }

        validateSensorPayloadIds(sensorPayload);

        SensorType sensorType =
                parseSensorType(sensorPayload.sensorType());

        Instant timestamp =
                parseTimestampOrNow(sensorPayload.time());

        SensorDataWriteCommand sensorDataWriteCommand = SensorDataWriteCommand.of(
                sensorPayload.organizationId(),
                sensorPayload.deviceEui(),
                sensorPayload.storageId(),
                sensorPayload.sectionId(),
                sensorType.value(),
                sensorPayload.value(),
                sensorType.unit(),
                timestamp
        );

        sensorInfluxRepository.save(sensorDataWriteCommand);
    }

    /**
     * 특정 구역의 센서별 최신 데이터를 조회한다.
     * <p>
     * 실제 구역 존재 여부는 검증하지 않는다.
     * 조회 결과가 없으면 빈 목록을 반환한다.
     */
    public List<SensorPayload> findLatestByZone(
            Long zoneId
    ) {
        validatePositiveId(zoneId, ZONE_ID);

        return sensorInfluxRepository.findLatestByZone(
                zoneId
        );
    }

    /**
     * 특정 창고에 속한 모든 구역의 센서별 최신 데이터를 조회한다.
     * <p>
     * 실제 창고 존재 여부는 검증하지 않는다.
     * 조회 결과가 없으면 빈 목록을 반환한다.
     */
    public List<SensorPayload> findLatestByStorage(
            Long storageId
    ) {
        validatePositiveId(storageId, STORAGE_ID);

        return sensorInfluxRepository.findLatestByStorage(
                storageId
        );
    }

    /**
     * 조직의 최신 센서 데이터를 조회한다.
     * <p>
     * sensorType이 없으면 모든 센서 타입을 조회한다.
     */
    public List<SensorPayload> findLatestByOrganization(
            Long organizationId,
            String sensorType
    ) {
        validatePositiveId(organizationId, ORGANIZATION_ID);

        String resolvedSensorType =
                parseOptionalSensorType(sensorType);

        if (resolvedSensorType == null) {
            return sensorInfluxRepository
                    .findLatestByOrganization(organizationId);
        }

        return sensorInfluxRepository
                .findLatestByOrganizationAndSensorType(
                        organizationId,
                        resolvedSensorType
                );
    }

    /**
     * 특정 구역의 센서 이력을 조회한다.
     * <p>
     * from이 없으면 종료 시각 기준 최근 24시간을 조회한다.
     * to가 없으면 현재 시각을 사용한다.
     * window가 없으면 10분 단위로 집계한다.
     * sensorType이 없으면 모든 센서 타입을 조회한다.
     */
    public List<SensorHistoryResponse> findHistoryByZone(
            Long zoneId,
            String sensorType,
            Instant from,
            Instant to,
            String window
    ) {
        validatePositiveId(zoneId, ZONE_ID);

        Instant resolvedTo = resolveTo(to);
        Instant resolvedFrom = resolveFrom(from, resolvedTo);
        String resolvedWindow = resolveWindow(window);
        String resolvedSensorType =
                parseOptionalSensorType(sensorType);

        validateTimeRange(resolvedFrom, resolvedTo);

        return sensorInfluxRepository.findHistoryByZone(
                zoneId,
                resolvedSensorType,
                resolvedFrom,
                resolvedTo,
                resolvedWindow
        );
    }

    /**
     * 저장할 센서 데이터의 ID 형식을 검증한다.
     * <p>
     * 실제 계층 관계는 데이터 수집 이전 단계에서 검증한다.
     */
    private void validateSensorPayloadIds(
            SensorPayload sensorPayload
    ) {
        validatePositiveId(
                sensorPayload.organizationId(),
                ORGANIZATION_ID
        );

        validatePositiveId(
                sensorPayload.storageId(),
                STORAGE_ID
        );

        validatePositiveId(
                sensorPayload.sectionId(),
                ZONE_ID
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

    /**
     * 선택적인 센서 타입을 변환한다.
     * <p>
     * 값이 없으면 모든 센서 타입을 조회하기 위해 null을 반환한다.
     */
    private String parseOptionalSensorType(
            String sensorType
    ) {
        if (sensorType == null || sensorType.isBlank()) {
            return null;
        }

        return parseSensorType(sensorType).value();
    }

    /**
     * 필수 센서 타입을 검증하고 SensorType으로 변환한다.
     */
    private SensorType parseSensorType(
            String sensorType
    ) {
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

    private String normalizeWindow(
            String window
    ) {
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

    private Instant parseTimestampOrNow(
            String time
    ) {
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
