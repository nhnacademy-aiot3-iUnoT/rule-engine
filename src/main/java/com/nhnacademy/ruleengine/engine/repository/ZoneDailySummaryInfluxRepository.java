package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.environment.DoorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.engine.repository.support.FluxQueryExecutor;
import com.nhnacademy.ruleengine.engine.repository.support.FluxRecords;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 하루 요약을 요약 버킷에 적재하고 다시 읽는다.
 *
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ZoneDailySummaryInfluxRepository {

    static final String MEASUREMENT = "zone_daily_summary";

    private static final String ZONE_ID = "zone_id";
    private static final String SENSOR_TYPE = "sensor_type";
    private static final String UNIT = "unit";
    private static final String DOOR_SENSOR_TYPE = "door";

    private static final String AVG = "avg";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String THRESHOLD_MIN = "threshold_min";
    private static final String THRESHOLD_MAX = "threshold_max";
    private static final String OUT_OF_RANGE_RATIO = "out_of_range_ratio";
    private static final String PREVIOUS_DAY_AVG = "previous_day_avg";
    private static final String OPEN_COUNT = "open_count";
    private static final String OPEN_MINUTES = "open_minutes";

    private final InfluxDBClient influxDBClient;
    private final InfluxDbProperties influxDbProperties;
    private final FluxQueryExecutor fluxQueryExecutor;

    /**
     * 하루 요약을 적재한다.
     * <p>
     * 수집된 데이터가 없는 날은 저장하지 않는다. 빈 요약을 남기면 "그날 센서가 멈췄다"와
     * "아직 적재하지 않았다"가 구분되지 않아 재적재 여부를 판단할 수 없게 된다.
     */
    public void save(ZoneDailySummary summary) {
        if (summary.hasNoData()) {
            log.info(
                    "수집된 데이터가 없어 요약을 저장하지 않습니다. zoneId={}, date={}",
                    summary.zoneId(),
                    summary.date()
            );

            return;
        }

        List<Point> points = new ArrayList<>();
        Instant time = ZoneDailySummary.startOfDay(summary.date());

        for (SensorDailyStat stat : summary.sensorStats()) {
            points.add(toSensorPoint(summary.zoneId(), time, stat));
        }

        if (summary.door() != null) {
            points.add(toDoorPoint(summary.zoneId(), time, summary.door()));
        }

        try {
            influxDBClient.getWriteApiBlocking().writePoints(
                    influxDbProperties.summaryBucket(),
                    influxDbProperties.org(),
                    points
            );
        } catch (Exception exception) {
            log.error(
                    "하루 요약 저장에 실패했습니다. zoneId={}, date={}",
                    summary.zoneId(),
                    summary.date(),
                    exception
            );

            throw new SensorDataSaveException(
                    "하루 요약 저장 실패",
                    exception
            );
        }
    }

    /**
     * 저장된 하루 요약을 날짜 구간(from 이상 to 이하)으로 조회한다.
     * <p>
     * 적재되지 않은 날짜는 결과에서 빠진다. 비어 있는 요약으로 채우면 데이터가 없던 날과
     * 배치가 실패한 날이 같아 보이므로, 그 판단은 호출한 쪽에 남긴다.
     */
    public List<ZoneDailySummary> findByZoneBetween(
            Long zoneId,
            LocalDate from,
            LocalDate to
    ) {
        String fluxQuery = """
                from(bucket: "%s")
                    |> range(
                        start: time(v: "%s"),
                        stop: time(v: "%s")
                    )
                    |> filter(fn: (r) => r._measurement == "%s")
                    |> filter(fn: (r) => r.%s == "%s")
                    |> pivot(
                        rowKey: ["_time"],
                        columnKey: ["_field"],
                        valueColumn: "_value"
                    )
                    |> group()
                    |> sort(columns: %s)
                """.formatted(
                influxDbProperties.summaryBucket(),
                ZoneDailySummary.startOfDay(from),
                // stop은 배타적이라 마지막 날을 포함하려면 그다음 날 자정을 끝으로 잡는다.
                ZoneDailySummary.startOfDay(to.plusDays(1)),
                MEASUREMENT,
                ZONE_ID,
                zoneId,
                FluxRecords.toColumns("_time", SENSOR_TYPE)
        );

        return toSummaries(zoneId, fluxQueryExecutor.query(fluxQuery));
    }

    public Optional<ZoneDailySummary> findByZoneAndDate(
            Long zoneId,
            LocalDate date
    ) {
        return findByZoneBetween(zoneId, date, date)
                .stream()
                .findFirst();
    }

    private Point toSensorPoint(
            Long zoneId,
            Instant time,
            SensorDailyStat stat
    ) {
        Point point = Point.measurement(MEASUREMENT)
                .addTag(ZONE_ID, String.valueOf(zoneId))
                .addTag(SENSOR_TYPE, stat.sensorType())
                .addTag(UNIT, stat.unit())
                .addField(AVG, stat.avg())
                .addField(MIN, stat.min())
                .addField(MAX, stat.max())
                .time(time, WritePrecision.NS);

        // 값이 없는 항목은 필드를 아예 만들지 않는다.
        // 0으로 채우면 "임계값이 없다"가 "임계값이 0이다"로 바뀌어 읽는 쪽이 오해한다.
        addFieldIfPresent(point, THRESHOLD_MIN, stat.thresholdMin());
        addFieldIfPresent(point, THRESHOLD_MAX, stat.thresholdMax());
        addFieldIfPresent(point, OUT_OF_RANGE_RATIO, stat.outOfRangeRatio());
        addFieldIfPresent(point, PREVIOUS_DAY_AVG, stat.previousDayAvg());

        return point;
    }

    private Point toDoorPoint(
            Long zoneId,
            Instant time,
            DoorDailyStat door
    ) {
        return Point.measurement(MEASUREMENT)
                .addTag(ZONE_ID, String.valueOf(zoneId))
                .addTag(SENSOR_TYPE, DOOR_SENSOR_TYPE)
                .addField(OPEN_COUNT, door.openCount())
                .addField(OPEN_MINUTES, door.openMinutes())
                .time(time, WritePrecision.NS);
    }

    private void addFieldIfPresent(
            Point point,
            String field,
            Double value
    ) {
        if (value != null) {
            point.addField(field, value);
        }
    }

    /**
     * pivot으로 한 줄이 된 레코드를 날짜별 요약으로 다시 묶는다.
     * <p>
     * 저장할 때 센서 타입마다 포인트를 나눴으므로 한 날짜가 여러 줄로 나뉘어 돌아온다.
     * 조회 결과가 시간순으로 정렬되어 있어 순서를 유지하는 Map으로 모으면 날짜 순서가 보존된다.
     */
    private List<ZoneDailySummary> toSummaries(
            Long zoneId,
            List<FluxRecord> records
    ) {
        Map<LocalDate, List<SensorDailyStat>> statsByDate = new LinkedHashMap<>();
        Map<LocalDate, DoorDailyStat> doorByDate = new LinkedHashMap<>();

        for (FluxRecord record : records) {
            if (record.getTime() == null) {
                continue;
            }

            LocalDate date = ZoneDailySummary.dateOf(record.getTime());

            // 날짜가 door 포인트로만 등장해도 요약이 만들어져야 하므로 미리 자리를 만든다.
            statsByDate.computeIfAbsent(date, key -> new ArrayList<>());

            if (DOOR_SENSOR_TYPE.equals(FluxRecords.getString(record, SENSOR_TYPE))) {
                doorByDate.put(date, toDoorStat(record));
                continue;
            }

            statsByDate.get(date).add(toSensorStat(record));
        }

        return statsByDate.entrySet()
                .stream()
                .map(entry -> new ZoneDailySummary(
                        zoneId,
                        entry.getKey(),
                        sortedBySensorType(entry.getValue()),
                        doorByDate.get(entry.getKey())
                ))
                .toList();
    }

    private List<SensorDailyStat> sortedBySensorType(List<SensorDailyStat> stats) {
        return stats.stream()
                .sorted(Comparator.comparing(SensorDailyStat::sensorType))
                .toList();
    }

    private SensorDailyStat toSensorStat(FluxRecord record) {
        return new SensorDailyStat(
                FluxRecords.getString(record, SENSOR_TYPE),
                FluxRecords.getString(record, UNIT),
                FluxRecords.requireDouble(record, AVG),
                FluxRecords.requireDouble(record, MIN),
                FluxRecords.requireDouble(record, MAX),
                FluxRecords.getDouble(record, THRESHOLD_MIN),
                FluxRecords.getDouble(record, THRESHOLD_MAX),
                FluxRecords.getDouble(record, OUT_OF_RANGE_RATIO),
                FluxRecords.getDouble(record, PREVIOUS_DAY_AVG)
        );
    }

    private DoorDailyStat toDoorStat(FluxRecord record) {
        return new DoorDailyStat(
                (long) FluxRecords.requireDouble(record, OPEN_COUNT),
                (long) FluxRecords.requireDouble(record, OPEN_MINUTES)
        );
    }

}
