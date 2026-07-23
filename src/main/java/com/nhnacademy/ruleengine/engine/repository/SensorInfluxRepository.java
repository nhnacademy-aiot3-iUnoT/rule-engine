package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.exception.SensorDataException;
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

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

    public List<SensorPayloadDto> findLatestBySectionId(Long organizationId, Long storageId, Long sectionId) {
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

    public List<SensorPayloadDto> findLatestOrganizationBySensorType(Long organizationId, String sensorType) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: -30d)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.sensor_type == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r._field == "value")
                    |> filter(fn: (r) => exists r.storage_id)
                    |> group(columns: ["storage_id"])
                    |> last()
                    |> sort(columns: ["storage_id"])
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
                |> group(columns: ["section_id", "sensor_type"])
                |> last()
                |> group()
                |> sort(columns: ["section_id", "sensor_type"])
            """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                organizationId
        );

        return executeQuery(fluxQuery);
    }



    private List<SensorPayloadDto> executeQuery(String fluxQuery) {
        try {
            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDbProperties.org())
                    .stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(this::toSensorPayloadDto)
                    .toList();
        } catch (Exception exception) {
            throw new SensorDataException(
                    ErrorCode.SENSOR_DATA_QUERY_FAILED
            );
        }
    }

    private SensorPayloadDto toSensorPayloadDto(FluxRecord record) {
        Object rawValue = record.getValue();

        if (!(rawValue instanceof Number numberValue)) {
            throw new IllegalStateException(
                    "센서 측정값이 숫자 형식이 아닙니다: " + rawValue
            );
        }

        return new SensorPayloadDto(
                parseId(record, "organization_id"),
                getStringValue(record, "device_eui"),
                parseId(record, "storage_id"),
                parseId(record, "section_id"),
                getStringValue(record, "sensor_type"),
                numberValue.doubleValue(),
                getStringValue(record, "unit"),
                record.getTime() != null
                        ? record.getTime().toString()
                        : null
        );
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

}
