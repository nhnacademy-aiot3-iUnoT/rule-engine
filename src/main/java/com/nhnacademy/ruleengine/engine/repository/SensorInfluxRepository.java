package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorDataWriteCommand;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
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
    private static final String SENSOR_TYPE = "sensor_type";

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;

    public void save(
            SensorDataWriteCommand command
    ) {
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("organization_id", String.valueOf(command.organizationId()))
                .addTag("storage_id", String.valueOf(command.storageId()))
                .addTag("section_id", String.valueOf(command.sectionId()))
                .addTag(SENSOR_TYPE, command.sensorType())
                .addTag("device_eui", command.deviceEui())
                .addTag("unit", command.unit())
                .addField(VALUE_FIELD, command.value())
                .time(command.timestamp(), WritePrecision.NS);

        try {
            influxDBClient.getWriteApiBlocking().writePoint(
                    influxDbProperties.bucket(),
                    influxDbProperties.org(),
                    point
            );
        } catch (Exception exception) {
            log.error(
                    "InfluxDB 센서 데이터 저장에 실패했습니다. organizationId={}, storageId={}, sectionId={}, device_eui={}",
                    command.organizationId(),
                    command.storageId(),
                    command.sectionId(),
                    command.deviceEui(),
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
    public List<SensorPayload> findLatestByZone(Long zoneId) {
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
                zoneId,
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
    public List<SensorHistoryResponse> findHistoryByZone(
            Long zoneId,
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
                zoneId,
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
            FluxRecord fluxRecord
    ) {
        return new SensorPayload(
                parseId(fluxRecord, "organization_id"),
                getStringValue(fluxRecord, "device_eui"),
                parseId(fluxRecord, "storage_id"),
                parseId(fluxRecord, "section_id"),
                getStringValue(fluxRecord, SENSOR_TYPE),
                getNumberValue(fluxRecord),
                resolveUnit(fluxRecord),
                fluxRecord.getTime() != null
                        ? fluxRecord.getTime().toString()
                        : null
        );
    }

    private SensorHistoryResponse toHistoryResponse(
            FluxRecord fluxRecord
    ) {
        if (fluxRecord.getTime() == null) {
            throw new IllegalStateException(
                    "센서 측정 시간이 존재하지 않습니다."
            );
        }

        return new SensorHistoryResponse(
                getStringValue(fluxRecord, SENSOR_TYPE),
                resolveUnit(fluxRecord),
                fluxRecord.getTime(),
                roundToFirstDecimalPlace(
                        getNumberValue(fluxRecord)
                )
        );
    }

    private String resolveUnit(
            FluxRecord fluxRecord
    ) {
        String sensorType = getStringValue(fluxRecord, SENSOR_TYPE);

        return SensorType.findByValue(sensorType)
                .map(SensorType::unit)
                .orElseGet(() -> getStringValue(fluxRecord, "unit"));
    }

    private double getNumberValue(FluxRecord fluxRecord) {
        Object rawValue = fluxRecord.getValue();

        if (!(rawValue instanceof Number numberValue)) {
            throw new IllegalStateException(
                    "센서 측정값이 숫자 형식이 아닙니다: "
                            + rawValue
            );
        }

        return numberValue.doubleValue();
    }

    private String getStringValue(
            FluxRecord fluxRecord,
            String key
    ) {
        Object value = fluxRecord.getValueByKey(key);

        return value != null
                ? String.valueOf(value)
                : null;
    }

    private Long parseId(
            FluxRecord fluxRecord,
            String key
    ) {
        String id = getStringValue(fluxRecord, key);

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
