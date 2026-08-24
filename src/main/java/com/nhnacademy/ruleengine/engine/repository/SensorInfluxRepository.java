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
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.engine.repository.support.FluxQueryExecutor;
import com.nhnacademy.ruleengine.engine.repository.support.FluxRecords;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SensorInfluxRepository {

    private static final String VALUE_FIELD = "value";
    private static final String DEFAULT_LATEST_RANGE = "-30d";

    private static final String ORGANIZATION_ID = "organization_id";
    private static final String STORAGE_ID = "storage_id";
    private static final String ZONE_ID = "zone_id";
    private static final String SENSOR_TYPE = "sensor_type";
    private static final String UNIT = "unit";
    private static final String DEVICE_EUI = "device_eui";

    private static final String DOOR_SENSOR_TYPE = "door";

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;
    private final FluxQueryExecutor fluxQueryExecutor;

    public void save(
            SensorDataWriteCommand command
    ) {
        Point point = Point.measurement(influxDbProperties.measurement())
                .addTag(ORGANIZATION_ID, String.valueOf(command.organizationId()))
                .addTag(STORAGE_ID, String.valueOf(command.storageId()))
                .addTag(ZONE_ID, String.valueOf(command.zoneId()))
                .addTag(SENSOR_TYPE, command.sensorType())
                .addTag(DEVICE_EUI, command.deviceEui())
                .addTag(UNIT, command.unit())
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
                    command.deviceEui()
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
        return findLatest(
                tagEquals(ZONE_ID, zoneId),
                SENSOR_TYPE
        );
    }

    /**
     * 특정 창고에 속한 구역별·센서 타입별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestByStorage(Long storageId) {
        return findLatest(
                tagEquals(STORAGE_ID, storageId),
                ZONE_ID, SENSOR_TYPE
        );
    }

    /**
     * 특정 조직의 센서별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestByOrganization(Long organizationId) {
        return findLatest(
                tagEquals(ORGANIZATION_ID, organizationId),
                STORAGE_ID, ZONE_ID, SENSOR_TYPE
        );
    }

    /**
     * 특정 조직에서 지정한 센서 타입의 구역별 최신 데이터를 조회한다.
     */
    public List<SensorPayload> findLatestByOrganizationAndSensorType(
            Long organizationId,
            String sensorType
    ) {
        return findLatest(
                tagEquals(ORGANIZATION_ID, organizationId) + tagEquals(SENSOR_TYPE, sensorType),
                STORAGE_ID, ZONE_ID
        );
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
        // sensorType이 명시적으로 "door"인 경우: 원본 값만 반환한다.
        if (DOOR_SENSOR_TYPE.equals(sensorType)) {
            return fluxQueryExecutor.query(
                    doorZoneHistoryQuery(zoneId, createSensorTypeFilter(sensorType), from, to),
                    this::toHistoryResponse
            );
        }

        // sensorType이 door가 아닌 특정 타입이거나 null(전체 조회)인 경우:
        // door를 제외한 나머지는 평균 집계로 조회한다.
        List<SensorHistoryResponse> aggregatedHistory = fluxQueryExecutor.query(
                defaultZoneHistoryQuery(zoneId, createSensorTypeFilter(sensorType), from, to, window),
                this::toHistoryResponse
        );

        // 특정 센서 타입(door 제외)만 조회한 경우는 그대로 반환한다.
        if (sensorType != null) {
            return aggregatedHistory;
        }

        // sensorType이 null(전체 조회)인 경우, door는 반드시 원본 값으로 별도 조회해서 합친다.
        List<SensorHistoryResponse> doorHistory = fluxQueryExecutor.query(
                doorZoneHistoryQuery(zoneId, createSensorTypeFilter(DOOR_SENSOR_TYPE), from, to),
                this::toHistoryResponse
        );

        return Stream.concat(
                aggregatedHistory.stream(),
                doorHistory.stream()
        ).toList();
    }

    /**
     * 구역의 기간별 센서 타입 통계조회
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
                    |> filter(fn: (r) => r.%s == "%s")
                    |> filter(fn: (r) => r.%s != "%s")
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: %s)
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
                    |> sort(columns: %s)
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                ZONE_ID,
                zoneId,
                SENSOR_TYPE,
                DOOR_SENSOR_TYPE,
                VALUE_FIELD,
                FluxRecords.toColumns(SENSOR_TYPE, UNIT),
                FluxRecords.toColumns(SENSOR_TYPE)
        );

        return fluxQueryExecutor.query(fluxQuery, this::toDailyAggregate);
    }

    /**
     * 기간 안에 데이터가 들어온 저장소 번호를 모두 찾는다.
     * <p>
     * 하루 요약 배치가 어떤 저장소를 돌아야 하는지는 결국 "데이터가 들어온 저장소"다.
     * 별도 목록을 관리하면 저장소가 늘거나 빠질 때마다 어긋나므로 원본에서 직접 뽑는다.
     */
    public List<Long> findStorageIds(
            Instant from,
            Instant to
    ) {
        return findTagIds(STORAGE_ID, "", from, to);
    }

    /**
     * 저장소에 속한 구역 번호를 찾는다.
     * <p>
     * 저장소와 구역의 관계는 인벤토리가 관리하지만, 요약이 필요한 것은 "그 기간에 실제로
     * 데이터를 보낸 구역"이다. 등록만 되고 센서가 없는 구역까지 돌면 빈 요약만 쌓인다.
     */
    public List<Long> findZoneIdsByStorage(
            Long storageId,
            Instant from,
            Instant to
    ) {
        return findTagIds(
                ZONE_ID,
                " and r.%s == \"%s\"".formatted(STORAGE_ID, storageId),
                from,
                to
        );
    }

    /**
     * 태그 값을 숫자 목록으로 읽는다. 태그 값만 읽으면 되는 질의라 측정값을 훑지 않는다.
     */
    private List<Long> findTagIds(
            String tag,
            String extraPredicate,
            Instant from,
            Instant to
    ) {
        String fluxQuery = """
                import "influxdata/influxdb/schema"

                schema.tagValues(
                    bucket: "%s",
                    tag: "%s",
                    predicate: (r) => r._measurement == "%s"%s,
                    start: time(v: "%s"),
                    stop: time(v: "%s")
                )
                """.formatted(
                influxDbProperties.bucket(),
                tag,
                influxDbProperties.measurement(),
                extraPredicate,
                from,
                to
        );

        return fluxQueryExecutor.query(fluxQuery, fluxRecord -> toId(fluxRecord, tag))
                .stream()
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    /**
     * 태그로 걸러 마지막 값만 뽑는 조회. 네 가지 최신 조회가 모두 같은 골격이라 한곳에 둔다.
     * <p>
     * 다른 것은 어떤 태그로 거르느냐와 무엇을 단위로 "마지막 하나"를 고르느냐뿐이다.
     * groupColumns가 곧 그 단위이며, 결과 정렬 기준으로도 같은 목록을 쓴다.
     */
    private List<SensorPayload> findLatest(
            String tagFilters,
            String... groupColumns
    ) {
        String columns = FluxRecords.toColumns(groupColumns);

        String fluxQuery = """
                from(bucket: "%s")
                    |> range(start: %s)
                    |> filter(fn: (r) => r._measurement == "%s")
                    %s
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: %s)
                    |> last()
                    |> group()
                    |> sort(columns: %s)
                """.formatted(
                influxDbProperties.bucket(),
                DEFAULT_LATEST_RANGE,
                influxDbProperties.measurement(),
                tagFilters,
                VALUE_FIELD,
                columns,
                columns
        );

        return fluxQueryExecutor.query(fluxQuery, this::toSensorPayload);
    }

    private String tagEquals(
            String tag,
            Object value
    ) {
        return "|> filter(fn: (r) => r.%s == \"%s\")%n".formatted(tag, value);
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
                    |> filter(fn: (r) => r.%s == "%s")
                    %s
                    |> filter(fn: (r) => r._field == "%s")
                    |> group()
                    |> sort(columns: %s)
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                ZONE_ID,
                zoneId,
                sensorTypeFilter,
                VALUE_FIELD,
                FluxRecords.toColumns("_time")
        );
    }

    private String defaultZoneHistoryQuery(
            Long zoneId,
            String sensorTypeFilter,
            Instant from,
            Instant to,
            String window
    ) {
        return """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.%s == "%s")
                    |> filter(fn: (r) => r.%s != "%s")
                    %s
                    |> filter(fn: (r) => r._field == "%s")
                    |> group(columns: %s)
                    |> aggregateWindow(
                        every: %s,
                        fn: mean,
                        createEmpty: false
                    )
                    |> group()
                    |> sort(columns: %s)
                """.formatted(
                influxDbProperties.bucket(),
                from,
                to,
                influxDbProperties.measurement(),
                ZONE_ID,
                zoneId,
                SENSOR_TYPE,
                DOOR_SENSOR_TYPE,
                sensorTypeFilter,
                VALUE_FIELD,
                FluxRecords.toColumns(SENSOR_TYPE, UNIT),
                window,
                FluxRecords.toColumns(SENSOR_TYPE, "_time")
        );
    }

    private String createSensorTypeFilter(String sensorType) {
        if (sensorType == null) {
            return "";
        }

        return tagEquals(SENSOR_TYPE, sensorType);
    }

    private SensorPayload toSensorPayload(FluxRecord fluxRecord) {
        return new SensorPayload(
                parseId(fluxRecord, ORGANIZATION_ID),
                FluxRecords.getString(fluxRecord, DEVICE_EUI),
                parseId(fluxRecord, STORAGE_ID),
                parseId(fluxRecord, ZONE_ID),
                FluxRecords.getString(fluxRecord, SENSOR_TYPE),
                FluxRecords.requireValue(fluxRecord),
                resolveUnit(fluxRecord),
                fluxRecord.getTime() != null
                        ? fluxRecord.getTime().toString()
                        : null
        );
    }

    private SensorHistoryResponse toHistoryResponse(FluxRecord fluxRecord) {
        if (fluxRecord.getTime() == null) {
            throw new IllegalStateException(
                    "센서 측정 시간이 존재하지 않습니다."
            );
        }

        return new SensorHistoryResponse(
                FluxRecords.getString(fluxRecord, SENSOR_TYPE),
                resolveUnit(fluxRecord),
                fluxRecord.getTime(),
                roundToFirstDecimalPlace(FluxRecords.requireValue(fluxRecord))
        );
    }

    private SensorDailyAggregate toDailyAggregate(FluxRecord fluxRecord) {
        return new SensorDailyAggregate(
                FluxRecords.getString(fluxRecord, SENSOR_TYPE),
                resolveUnit(fluxRecord),
                (long) FluxRecords.requireDouble(fluxRecord, "count"),
                roundToFirstDecimalPlace(FluxRecords.requireDouble(fluxRecord, "avg")),
                roundToFirstDecimalPlace(FluxRecords.requireDouble(fluxRecord, "min")),
                roundToFirstDecimalPlace(FluxRecords.requireDouble(fluxRecord, "max"))
        );
    }

    // 태그 값은 문자열이므로 숫자가 아닌 값이 섞여 있어도 배치 전체를 멈추지 않고 건너뛴다.
    private Long toId(
            FluxRecord fluxRecord,
            String tag
    ) {
        Object value = fluxRecord.getValue();

        if (value == null) {
            return null;
        }

        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException exception) {
            log.warn("{} 태그가 숫자 형식이 아니라 건너뜁니다. value={}", tag, value);

            return null;
        }
    }

    private String resolveUnit(FluxRecord fluxRecord) {
        String sensorType = FluxRecords.getString(fluxRecord, SENSOR_TYPE);

        return SensorType.findByValue(sensorType)
                .map(SensorType::unit)
                .orElseGet(() -> FluxRecords.getString(fluxRecord, UNIT));
    }

    private Long parseId(
            FluxRecord fluxRecord,
            String key
    ) {
        String id = FluxRecords.getString(fluxRecord, key);

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

    private double roundToFirstDecimalPlace(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
