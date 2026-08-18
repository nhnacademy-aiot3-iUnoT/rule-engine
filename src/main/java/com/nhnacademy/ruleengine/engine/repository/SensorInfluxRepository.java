package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorDataWriteCommand;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorDailyAggregate;
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
import java.util.stream.Stream;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorInfluxRepository {

    private static final String VALUE_FIELD = "value";
    private static final String DEFAULT_LATEST_RANGE = "-30d";
    private static final String SENSOR_TYPE = "sensor_type";
    private static final String DOOR_SENSOR_TYPE = "door";

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;

    public void save(
            SensorDataWriteCommand command
    ) {
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag("organization_id", String.valueOf(command.organizationId()))
                .addTag("storage_id", String.valueOf(command.storageId()))
                .addTag("zone_id", String.valueOf(command.zoneId()))
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
                    "InfluxDB 센서 데이터 저장에 실패했습니다. organizationId={}, storageId={}, zoneId={}, device_eui={}",
                    command.organizationId(),
                    command.storageId(),
                    command.zoneId(),
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
                    |> filter(fn: (r) => r.zone_id == "%s")
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
                    |> group(columns: ["zone_id", "sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["zone_id", "sensor_type"])
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
                    |> group(columns: ["storage_id", "zone_id", "sensor_type"])
                    |> last()
                    |> group()
                    |> sort(columns: ["storage_id", "zone_id", "sensor_type"])
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
                    |> group(columns: ["storage_id", "zone_id"])
                    |> last()
                    |> group()
                    |> sort(columns: ["storage_id", "zone_id"])
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
     * <p>
     * door 센서는 이진 상태값(열림/닫힘)이라 평균 집계가 의미 없으므로
     * 집계 없이 원본 값(0/1) 그대로 조회한다. 그 외 센서는 window 단위로 평균 집계한다.
     */
    public List<SensorHistoryResponse> findHistoryByZone(
            Long zoneId,
            String sensorType,
            Instant from,
            Instant to,
            String window
    ) {
        // sensorType이 명시적으로 "door"인 경우: 원본 값만 반환한다.
        if (DOOR_SENSOR_TYPE.equals(sensorType)) {
            return executeQuery(
                    doorZoneHistoryQuery(
                            zoneId,
                            createSensorTypeFilter(sensorType),
                            from,
                            to
                    ),
                    this::toHistoryResponse
            );
        }

        // sensorType이 door가 아닌 특정 타입이거나 null(전체 조회)인 경우:
        // door를 제외한 나머지는 평균 집계로 조회한다.
        List<SensorHistoryResponse> aggregatedHistory = executeQuery(
                defaultZoneHistoryQuery(
                        zoneId,
                        createSensorTypeFilter(sensorType),
                        from,
                        to,
                        window
                ),
                this::toHistoryResponse
        );

        // 특정 센서 타입(door 제외)만 조회한 경우는 그대로 반환한다.
        if (sensorType != null) {
            return aggregatedHistory;
        }

        // sensorType이 null(전체 조회)인 경우, door는 반드시 원본 값으로 별도 조회해서 합친다.
        // 그렇지 않으면 door가 defaultZoneHistoryQuery의 10분 평균 집계에 걸려
        // "10분 간격으로 열렸다"는 식의 부정확한 타임스탬프/값이 만들어진다.
        List<SensorHistoryResponse> doorHistory = executeQuery(
                doorZoneHistoryQuery(
                        zoneId,
                        createSensorTypeFilter(DOOR_SENSOR_TYPE),
                        from,
                        to
                ),
                this::toHistoryResponse
        );

        return Stream.concat(
                aggregatedHistory.stream(),
                doorHistory.stream()
        ).toList();
    }

    /**
     * 구역의 기간별 센서 타입 통계(표본 수/평균/최소/최대)를 한 번의 질의로 집계한다.
     * <p>
     * 원본을 애플리케이션으로 끌어와 계산하면 하루치 전부를 메모리에 올려야 하므로
     * reduce로 InfluxDB 안에서 한 번만 훑고 끝낸다.
     * door는 열림/닫힘 이진값이라 평균·최소·최대가 의미 없으므로 제외한다.
     */
    public List<SensorDailyAggregate> findDailyAggregatesByZone(
            Long zoneId,
            Instant from,
            Instant to
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.zone_id == "%s")
                    |> filter(fn: (r) => r.sensor_type != "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: ["sensor_type", "unit"])
                    |> reduce(
                        identity: {count: 0.0, sum: 0.0, min: 0.0, max: 0.0},
                        fn: (r, accumulator) => ({
                            count: accumulator.count + 1.0,
                            sum: accumulator.sum + r._value,
                            min: if accumulator.count == 0.0 then r._value
                                 else if r._value < accumulator.min then r._value
                                 else accumulator.min,
                            max: if accumulator.count == 0.0 then r._value
                                 else if r._value > accumulator.max then r._value
                                 else accumulator.max
                        })
                    )
                    |> map(fn: (r) => ({r with avg: r.sum / r.count}))
                    |> group()
                    |> sort(columns: ["sensor_type"])
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                zoneId,
                DOOR_SENSOR_TYPE,
                VALUE_FIELD
        );

        return executeQuery(fluxQuery, this::toDailyAggregate);
    }

    /**
     * 임계 범위를 벗어난 측정값의 개수를 센다.
     * <p>
     * 이탈 비율을 내려면 이탈 표본 수가 필요한데, 원본을 받아 세면 하루치를 전부 옮겨야 한다.
     * 개수만 필요하므로 InfluxDB에서 세어 숫자 하나만 받는다.
     * min이나 max 한쪽만 설정된 구역도 있어 조건은 설정된 경계만으로 만든다.
     */
    public long countOutOfRange(
            Long zoneId,
            String sensorType,
            Double min,
            Double max,
            Instant from,
            Instant to
    ) {
        String boundaryFilter = createBoundaryFilter(min, max);

        if (boundaryFilter.isEmpty()) {
            return 0L;
        }

        String fluxQuery = """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.zone_id == "%s")
                    |> filter(fn: (r) => r.sensor_type == "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    %s
                    |> group()
                    |> count()
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                zoneId,
                sensorType,
                VALUE_FIELD,
                boundaryFilter
        );

        // 이탈이 하나도 없으면 count()가 빈 결과를 돌려준다.
        return executeQuery(fluxQuery, this::toCount)
                .stream()
                .findFirst()
                .orElse(0L);
    }

    private String createBoundaryFilter(Double min, Double max) {
        if (min != null && max != null) {
            return "|> filter(fn: (r) => r._value < %s or r._value > %s)".formatted(min, max);
        }

        if (min != null) {
            return "|> filter(fn: (r) => r._value < %s)".formatted(min);
        }

        if (max != null) {
            return "|> filter(fn: (r) => r._value > %s)".formatted(max);
        }

        return "";
    }

    /**
     * door 센서의 원본(raw) 이력을 집계·필터 없이 조회한다.
     * 열림(1)/닫힘(0) 전환 시점을 정확히 판별하려면 두 값 모두 필요하므로
     * 값에 대한 필터를 걸지 않는다.
     */
    private String doorZoneHistoryQuery(
            Long zoneId,
            String sensorTypeFilter,
            Instant from,
            Instant to
    ) {
        return """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.zone_id == "%s")
                    %s
                    |> filter(fn: (r) => r._field == "%s")
                    |> group()
                    |> sort(columns: ["_time"])
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                zoneId,
                sensorTypeFilter,
                VALUE_FIELD
        );
    }

    private String defaultZoneHistoryQuery(
            Long zoneId,
            String sensorTypeFilter,
            Instant from,
            Instant to,
            String window) {
        return """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.zone_id == "%s")
                    |> filter(fn: (r) => r.sensor_type != "%s")
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
                DOOR_SENSOR_TYPE,
                sensorTypeFilter,
                VALUE_FIELD,
                window
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
                parseId(fluxRecord, "zone_id"),
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

    private SensorDailyAggregate toDailyAggregate(
            FluxRecord fluxRecord
    ) {
        return new SensorDailyAggregate(
                getStringValue(fluxRecord, SENSOR_TYPE),
                resolveUnit(fluxRecord),
                (long) getDoubleByKey(fluxRecord, "count"),
                roundToFirstDecimalPlace(getDoubleByKey(fluxRecord, "avg")),
                roundToFirstDecimalPlace(getDoubleByKey(fluxRecord, "min")),
                roundToFirstDecimalPlace(getDoubleByKey(fluxRecord, "max"))
        );
    }

    private Long toCount(FluxRecord fluxRecord) {
        return (long) getNumberValue(fluxRecord);
    }

    private double getDoubleByKey(
            FluxRecord fluxRecord,
            String key
    ) {
        Object rawValue = fluxRecord.getValueByKey(key);

        if (!(rawValue instanceof Number numberValue)) {
            throw new IllegalStateException(
                    key + " 집계값이 숫자 형식이 아닙니다: " + rawValue
            );
        }

        return numberValue.doubleValue();
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