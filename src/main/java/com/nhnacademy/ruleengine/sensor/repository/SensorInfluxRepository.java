package com.nhnacademy.ruleengine.sensor.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.query.FluxRecord;
import com.influxdb.client.write.Point;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import com.nhnacademy.ruleengine.sensor.dto.SensorReading;
import com.nhnacademy.ruleengine.sensor.exception.SensorDataQueryException;
import com.nhnacademy.ruleengine.sensor.exception.SensorDataSaveException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
// 센서 데이터를 InfluxDB Point로 변환해 저장한다.
public class SensorInfluxRepository {

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;

    public void save(
            String applicationName,
            String location,
            String sensorType,
            double value,
            String unit,
            String deviceName,
            String deviceEui,
            Instant timestamp
    ) {
        // 조회 조건은 tag로, 실제 측정값은 field로 구성한다.
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("application_name", applicationName)
                .addTag("location", location)
                .addTag("sensor_type", sensorType)
                .addTag("device_name", deviceName)
                .addTag("device_eui", deviceEui)
                .addTag("unit", unit)
                .addField("value", value)
                .time(timestamp, WritePrecision.NS);
        try {
            // 즉시 결과를 확인할 수 있도록 blocking write API를 사용한다.
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

    public List<SensorReading> findLatestByLocation(String location) {
        String query = """
                from(bucket: "%s")
                  |> range(start: 0)
                  |> filter(fn: (r) => r._measurement == "%s")
                  |> filter(fn: (r) => r.location == "%s")
                  |> filter(fn: (r) => r._field == "value")
                  |> group(columns: ["sensor_type"])
                  |> last()
                """.formatted(
                escapeFluxString(influxDbProperties.bucket()),
                escapeFluxString(influxDbProperties.measurement()),
                escapeFluxString(location)
        );

        try {
            QueryApi queryApi = influxDBClient.getQueryApi();
            return queryApi.query(query, influxDbProperties.org()).stream()
                    .flatMap(table -> table.getRecords().stream())
                    .map(this::toSensorReading)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception e) {
            throw new SensorDataQueryException(e);
        }
    }

    private SensorReading toSensorReading(FluxRecord record) {
        Object value = record.getValue();
        String sensorType = stringValue(record, "sensor_type");

        if (!(value instanceof Number number)
                || sensorType == null
                || record.getTime() == null) {
            log.warn("유효하지 않은 InfluxDB 조회 결과를 건너뜁니다: {}", record.getValues());
            return null;
        }

        return new SensorReading(
                sensorType,
                number.doubleValue(),
                stringValue(record, "unit"),
                stringValue(record, "device_name"),
                stringValue(record, "device_eui"),
                record.getTime()
        );
    }

    private String stringValue(FluxRecord record, String key) {
        Object value = record.getValueByKey(key);
        return value == null ? null : value.toString();
    }

    private String escapeFluxString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }
}
