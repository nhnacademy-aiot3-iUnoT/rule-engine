package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
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

    private static final String VALUE_FIELD = "value";
    private static final String DEFAULT_LATEST_RANGE = "-30d";

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
                .addField(VALUE_FIELD, value)
                .time(timestamp, WritePrecision.NS);

        try {
            influxDBClient.getWriteApiBlocking().writePoint(
                    influxDbProperties.bucket(),
                    influxDbProperties.org(),
                    point
            );
        } catch (Exception exception) {
            log.error(
                    "InfluxDB 센서 데이터 저장에 실패했습니다. organizationId={}, storageId={}, sectionId={}, deviceEui={}",
                    organizationId,
                    storageId,
                    sectionId,
                    deviceEui,
                    exception
            );

            throw new SensorDataSaveException(
                    "InfluxDB 저장 실패",
                    exception
            );
        }
    }

    /**
     * 특정 구역의 센서 타입별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestBySection(Long sectionId) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: %s)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.section_id == "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: ["sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                DEFAULT_LATEST_RANGE,
                influxDbProperties.measurement(),
                sectionId,
                VALUE_FIELD
        );

        return executeQuery(fluxQuery);
    }

    /**
     * 특정 창고에 속한 구역별·센서 타입별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestByStorage(Long storageId) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: %s)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.storage_id == "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: ["section_id", "sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["section_id", "sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                DEFAULT_LATEST_RANGE,
                influxDbProperties.measurement(),
                storageId,
                VALUE_FIELD
        );

        return executeQuery(fluxQuery);
    }

    /**
     * 특정 조직의 센서별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestByOrganization(
            Long organizationId
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: %s)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: ["storage_id", "section_id", "sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["storage_id", "section_id", "sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                DEFAULT_LATEST_RANGE,
                influxDbProperties.measurement(),
                organizationId,
                VALUE_FIELD
        );

        return executeQuery(fluxQuery);
    }

    /**
     * 특정 조직에서 지정한 센서 타입의 구역별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestByOrganizationAndSensorType(
            Long organizationId,
            String sensorType
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: %s)
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.organization_id == "%s")
                    |> filter(fn: (r) => r.sensor_type == "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: ["storage_id", "section_id"])
                    |> last()
                    |> group()
                    |> sort(columns: ["storage_id", "section_id"])
                """.formatted(
                influxDbProperties.bucket(),
                DEFAULT_LATEST_RANGE,
                influxDbProperties.measurement(),
                organizationId,
                sensorType,
                VALUE_FIELD
        );

        return executeQuery(fluxQuery);
    }

    /**
     * 특정 구역의 센서 이력을 조회한다.
     */
    public List<SensorHistoryResponse> findHistoryBySection(
            Long sectionId,
            String sensorType,
            Instant from,
            Instant to,
            String window
    ) {
        String sensorTypeFilter = createSensorTypeFilter(sensorType);

        String fluxQuery = """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.section_id == "%s")
                    %s
                    |> filter(fn: (r) => r._field == "%s")
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
                sectionId,
                sensorTypeFilter,
                VALUE_FIELD,
                window
        );

        return executeQuery(
                fluxQuery,
                this::toHistoryResponse
        );
    }

    private String createSensorTypeFilter(String sensorType) {
        if (sensorType == null) {
            return "";
        }

        return """
                |> filter(fn: (r) => r.sensor_type == "%s")
                """.formatted(sensorType);
    }

    private List<SensorPayload> executeQuery(String fluxQuery) {
        return executeQuery(
                fluxQuery,
                this::toSensorPayload
        );
    }

    private <T> List<T> executeQuery(
            String fluxQuery,
            Function<FluxRecord, T> mapper
    ) {
        try {
            return influxDBClient.getQueryApi()
                    .query(
                            fluxQuery,
                            influxDbProperties.org()
                    )
                    .stream()
                    .flatMap(table ->
                            table.getRecords().stream()
                    )
                    .map(mapper)
                    .toList();
        } catch (Exception exception) {
            log.error(
                    "InfluxDB 센서 데이터 조회에 실패했습니다.",
                    exception
            );

            throw new SensorDataException(
                    ErrorCode.SENSOR_DATA_QUERY_FAILED
            );
        }
    }

    private SensorPayload toSensorPayload(
            FluxRecord record
    ) {
        return new SensorPayload(
                parseId(record, "organization_id"),
                getStringValue(record, "device_eui"),
                parseId(record, "storage_id"),
                parseId(record, "section_id"),
                getStringValue(record, "sensor_type"),
                getNumberValue(record),
                getStringValue(record, "unit"),
                record.getTime() != null
                        ? record.getTime().toString()
                        : null
        );
    }

    private SensorHistoryResponse toHistoryResponse(
            FluxRecord record
    ) {
        if (record.getTime() == null) {
            throw new IllegalStateException(
                    "센서 측정 시간이 존재하지 않습니다."
            );
        }

        return new SensorHistoryResponse(
                getStringValue(record, "sensor_type"),
                getStringValue(record, "unit"),
                record.getTime(),
                roundToFirstDecimalPlace(
                        getNumberValue(record)
                )
        );
    }

    private double getNumberValue(FluxRecord record) {
        Object rawValue = record.getValue();

        if (!(rawValue instanceof Number numberValue)) {
            throw new IllegalStateException(
                    "센서 측정값이 숫자 형식이 아닙니다: "
                            + rawValue
            );
        }

        return numberValue.doubleValue();
    }

    private String getStringValue(
            FluxRecord record,
            String key
    ) {
        Object value = record.getValueByKey(key);

        return value != null
                ? String.valueOf(value)
                : null;
    }

    private Long parseId(
            FluxRecord record,
            String key
    ) {
        String id = getStringValue(record, key);

        if (id == null || id.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(id);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                    key + "가 숫자 형식이 아닙니다: " + id,
                    exception
            );
        }
    }

    private double roundToFirstDecimalPlace(
            double value
    ) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}