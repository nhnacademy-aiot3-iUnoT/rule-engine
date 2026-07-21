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
            Long locationId,
            Long positionId,
            String sensorType,
            double value,
            String unit,
            Instant timestamp
    ) {
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("organization_id", String.valueOf(organizationId))
                .addTag("location_id", String.valueOf(locationId))
                .addTag("position_id", String.valueOf(positionId))
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

    public List<SensorPayloadDto> findLatestByLocationId(Long locationId) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: -30d)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.location_id == "%s")
                    |> filter(fn: (r) => r._field == "value")
                    |> group(columns: ["sensor_type"])
                    |> last()
                    |> sort(columns: ["sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                locationId
        );

        return executeQuery(fluxQuery);
    }

    public List<SensorPayloadDto> findLatestBySensorType(String sensorType) {
        String fluxQuery = """
            from(bucket: "%s")
                |> range(start: -30d)
                |> filter(fn: (r) => r._measurement == "%s")
                |> filter(fn: (r) => r.sensor_type == "%s")
                |> filter(fn: (r) => r._field == "value")
                |> filter(fn: (r) => exists r.location_id)
                |> group(columns: ["location_id"])
                |> last()
                |> sort(columns: ["location_id"])
            """.formatted(
                influxDbProperties.bucket(),
                influxDbProperties.measurement(),
                sensorType
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
                parseId(record, "location_id"),
                parseId(record, "position_id"),
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
