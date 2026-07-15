package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
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
            Long locationId,
            String applicationName,
            String location,
            String sensorType,
            double value,
            String unit,
            String deviceName,
            String deviceEui,
            Instant timestamp
    ) {
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("location_id", String.valueOf(locationId))
                .addTag("application_name", applicationName)
                .addTag("location", location)
                .addTag("sensor_type", sensorType)
                .addTag("device_name", deviceName)
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

        try {
            return influxDBClient.getQueryApi()
                    .query(fluxQuery, influxDbProperties.org())
                    .stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(this::toSensorPayloadDto)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "InfluxDB 센서 데이터 조회 실패. locationId=" + locationId,
                    e
            );
        }
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

        return influxDBClient.getQueryApi()
                .query(fluxQuery, influxDbProperties.org())
                .stream()
                .flatMap(table -> table.getRecords().stream())
                .map(this::toSensorPayloadDto)
                .toList();
    }


    private SensorPayloadDto toSensorPayloadDto(FluxRecord record) {
        Object rawValue = record.getValue();

        if (!(rawValue instanceof Number numberValue)) {
            throw new IllegalStateException(
                    "센서 측정값이 숫자 형식이 아닙니다: " + rawValue
            );
        }

        return new SensorPayloadDto(
                getStringValue(record, "application_name"),
                getStringValue(record, "device_name"),
                getStringValue(record, "device_eui"),
                getStringValue(record, "location"),
                parseLocationId(record),
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

    private Long parseLocationId(FluxRecord record) {
        String locationId = getStringValue(record, "location_id");

        if (locationId == null || locationId.isBlank()) {
            return null;
        }

        return Long.parseLong(locationId);
    }
}