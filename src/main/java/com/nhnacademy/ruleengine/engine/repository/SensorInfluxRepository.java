package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorInfluxRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;

    public void save(
            Long organizationId,
            String deviceEui,
            Long storageId,
            Long sectionId,
            String sensorType,
            double value,
            String unit,
            Instant timestamp
    ) {
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("organization_id", String.valueOf(organizationId))
                .addTag("storage_id", String.valueOf(storageId))
                .addTag("section_id", String.valueOf(sectionId))
                .addTag("sensor_type", sensorType)
                .addTag("device_eui", deviceEui)
                .addTag("unit", unit)
                .addField("value", value)
                .time(timestamp, WritePrecision.NS);

        try {
            influxDBClient.getWriteApiBlocking()
                    .writePoint(
                            influxDbProperties.bucket(),
                            influxDbProperties.org(),
                            point
                    );
        } catch (Exception e) {
            throw new SensorDataSaveException("InfluxDB 저장 실패", e);
        }
    }

    public List<SensorPayloadDto> findLatestBySectionId(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: -30d)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.section_id == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r.storage_id == "%s")
                    |> filter(fn: (r) => r._field == "value")
                    |> group(columns: ["sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                sectionId,
                organizationId,
                storageId
        );

        return executeQuery(fluxQuery);
    }

    public List<SensorPayloadDto> findLatestByOrganizationAndSensorType(
            Long organizationId,
            String sensorType
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: -30d)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.sensor_type == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r._field == "value")
                    |> filter(fn: (r) => exists r.storage_id)
                    |> group(columns: ["storage_id", "section_id"])
                    |> last()
                    |> group()
                    |> sort(columns: ["storage_id", "section_id"])
                """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                sensorType,
                organizationId
        );

        return executeQuery(fluxQuery);
    }

    public List<SensorPayloadDto> findLatestByStorageId(
            Long organizationId,
            Long storageId
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: -30d)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r.storage_id == "%s")
                    |> filter(fn: (r) => r._field == "value")
                    |> group(columns: ["section_id", "sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["section_id", "sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                organizationId,
                storageId
        );

        return executeQuery(fluxQuery);
    }

    public List<SensorPayloadDto> findLatestByOrganizationId(Long organizationId) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: -30d)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r._field == "value")
                    |> group(columns: ["storage_id", "section_id", "sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["storage_id", "section_id", "sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                organizationId
        );

        return executeQuery(fluxQuery);
    }

    public List<SensorHistoryResponse> findHistory(
            Long organizationId,
            Long storageId,
            Long sectionId,
            String sensorType,
            Instant from,
            Instant to,
            String window
    ) {
        String sensorTypeFilter = sensorType == null
                ? ""
                : "|> filter(fn: (r) => r.sensor_type == \"%s\")"
                  .formatted(sensorType);

        String fluxQuery = """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r.storage_id == "%s")
                    |> filter(fn: (r) => r.section_id == "%s")
                    %s
                    |> filter(fn: (r) => r._field == "value")
                    |> group(columns: ["sensor_type", "unit"])
                    |> aggregateWindow(
                        every: %s,
                        fn: mean,
                        createEmpty: false
                    )
                    |> group()
                    |> sort(columns: ["sensor_type", "_time"])
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                organizationId,
                storageId,
                sectionId,
                sensorTypeFilter,
                window
        );

        return executeQuery(fluxQuery, this::toHistoryResponse);
    }

    private List<SensorPayloadDto> executeQuery(String fluxQuery) {
        return executeQuery(fluxQuery, this::toSensorPayloadDto);
    }

    private <T> List<T> executeQuery(
            String fluxQuery,
            Function<FluxRecord, T> mapper
    ) {
        try {
            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDbProperties.org())
                    .stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(mapper)
                    .toList();
        } catch (Exception exception) {
            log.error("InfluxDB 센서 데이터 조회에 실패했습니다.", exception);
            throw new SensorDataException(ErrorCode.SENSOR_DATA_QUERY_FAILED);
        }
    }

    private SensorPayloadDto toSensorPayloadDto(FluxRecord record) {
        return new SensorPayloadDto(
                parseId(record, "organization_id"),
                getStringValue(record, "device_eui"),
                parseId(record, "storage_id"),
                parseId(record, "section_id"),
                getStringValue(record, "sensor_type"),
                getNumberValue(record),
                getStringValue(record, "unit"),
                record.getTime() != null ? record.getTime().toString() : null
        );
    }

    private SensorHistoryResponse toHistoryResponse(FluxRecord record) {
        if (record.getTime() == null) {
            throw new IllegalStateException("센서 측정 시간이 존재하지 않습니다.");
        }

        return new SensorHistoryResponse(
                getStringValue(record, "sensor_type"),
                getStringValue(record, "unit"),
                record.getTime(),
                roundToFirstDecimalPlace(getNumberValue(record)) //소수점 둘째 자리 반올림
        );
    }

    private double getNumberValue(FluxRecord record) {
        Object rawValue = record.getValue();

        if (!(rawValue instanceof Number numberValue)) {
            throw new IllegalStateException(
                    "센서 측정값이 숫자 형식이 아닙니다: " + rawValue
            );
        }

        return numberValue.doubleValue();
    }

    private String getStringValue(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value != null ? String.valueOf(value) : null;
    }

    private Long parseId(FluxRecord record, String key) {
        String id = getStringValue(record, key);

        if (id == null || id.isBlank()) {
            return null;
        }

        return Long.parseLong(id);
    }

    private double roundToFirstDecimalPlace(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
