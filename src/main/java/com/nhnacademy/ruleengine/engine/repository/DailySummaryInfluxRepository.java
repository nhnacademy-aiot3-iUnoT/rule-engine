package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.environment.DoorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.StorageDailySummary;
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
import java.util.*;

/**
 * 하루 요약을 요약 버킷에 적재하고 다시 읽는다.
 *
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DailySummaryInfluxRepository {

    static final String MEASUREMENT = "zone_daily_summary";

    private static final String STORAGE_ID = "storage_id";
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
     * 저장소의 하루 요약을 저장한다
     */
    public void save(StorageDailySummary summary) {
        if (summary.hasNoData()) {
            log.info(
                    "수집된 데이터가 없어 요약을 저장하지 않습니다. storageId={}, date={}",
                    summary.storageId(),
                    summary.date()
            );

            return;
        }

        Instant time = ZoneDailySummary.startOfDay(summary.date());
        List<Point> points = new ArrayList<>();

        for (ZoneDailySummary zone : summary.zones()) {
            for (SensorDailyStat stat : zone.sensorStats()) {
                points.add(toSensorPoint(summary.storageId(), zone.zoneId(), time, stat));
            }

            if (zone.door() != null) {
                points.add(toDoorPoint(summary.storageId(), zone.zoneId(), time, zone.door()));
            }
        }

        try {
            influxDBClient.getWriteApiBlocking().writePoints(
                    influxDbProperties.summaryBucket(),
                    influxDbProperties.org(),
                    points
            );
        } catch (Exception exception) {
            log.error(
                    "하루 요약 저장에 실패했습니다. storageId={}, date={}",
                    summary.storageId(),
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
     * 저장된 요약을 날짜 구간(from 이상 to 이하)으로 조회한다.
     */
    public List<StorageDailySummary> findByStorageBetween(
            Long storageId,
            LocalDate from,
            LocalDate to
    ) {
        return toStorageSummaries(
                storageId,
                query(tagEquals(STORAGE_ID, storageId), from, to)
        );
    }


    /**
     * 구역 하나의 저장된 요약을 찾는다. 전일 평균 비교처럼 구역 단위로만 필요한 곳이 쓴다.
     */
    public Optional<ZoneDailySummary> findByZoneAndDate(
            Long zoneId,
            LocalDate date
    ) {
        return toZoneSummaries(query(tagEquals(ZONE_ID, zoneId), date, date))
                .values()
                .stream()
                .findFirst();
    }

    private List<FluxRecord> query(
            String tagFilter,
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
                    %s
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
                tagFilter,
                FluxRecords.toColumns("_time", ZONE_ID, SENSOR_TYPE)
        );

        return fluxQueryExecutor.query(fluxQuery);
    }

    private String tagEquals(
            String tag,
            Object value
    ) {
        return "|> filter(fn: (r) => r.%s == \"%s\")".formatted(tag, value);
    }

    private Point toSensorPoint(
            Long storageId,
            Long zoneId,
            Instant time,
            SensorDailyStat stat
    ) {
        Point point = Point.measurement(MEASUREMENT)
                .addTag(STORAGE_ID, String.valueOf(storageId))
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
            Long storageId,
            Long zoneId,
            Instant time,
            DoorDailyStat door
    ) {
        return Point.measurement(MEASUREMENT)
                .addTag(STORAGE_ID, String.valueOf(storageId))
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
     * pivot으로 한 줄이 된 레코드를 날짜별 저장소 요약으로 묶는다.
     */
    private List<StorageDailySummary> toStorageSummaries(
            Long storageId,
            List<FluxRecord> records
    ) {
        Map<LocalDate, List<ZoneDailySummary>> zonesByDate = new LinkedHashMap<>();

        toZoneSummaries(records).forEach((zoneDay, zone) -> zonesByDate
                .computeIfAbsent(zoneDay.date(), key -> new ArrayList<>())
                .add(zone));

        return zonesByDate.entrySet()
                .stream()
                .map(entry -> new StorageDailySummary(
                        storageId,
                        entry.getKey(),
                        List.copyOf(entry.getValue())
                ))
                .toList();
    }

    /**
     * 레코드를 구역과 날짜별로 묶는다.
     * <p>
     * 저장할 때 센서 타입마다 포인트를 나눴으므로 구역 하나의 하루가 여러 줄로 나뉘어 돌아온다.
     * 조회 결과가 날짜와 구역 순으로 정렬되어 있어, 순서를 유지하는 Map으로 모으면 그 순서가 보존된다.
     */
    private Map<ZoneDay, ZoneDailySummary> toZoneSummaries(List<FluxRecord> records) {
        Map<ZoneDay, List<SensorDailyStat>> statsByZoneDay = new LinkedHashMap<>();
        Map<ZoneDay, DoorDailyStat> doorByZoneDay = new LinkedHashMap<>();

        for (FluxRecord data : records) {
            if (data.getTime() == null) {
                continue;
            }

            ZoneDay key = new ZoneDay(
                    parseId(data, ZONE_ID),
                    ZoneDailySummary.dateOf(data.getTime())
            );

            if (key.zoneId() == null) {
                log.warn("구역 번호가 없는 요약이라 건너뜁니다. time={}", data.getTime());
            }

            // 구역이 door 포인트로만 등장해도 요약이 만들어져야 하므로 미리 자리를 만든다.
            statsByZoneDay.computeIfAbsent(key, ignored -> new ArrayList<>());

            if (DOOR_SENSOR_TYPE.equals(FluxRecords.getString(data, SENSOR_TYPE))) {
                doorByZoneDay.put(key, toDoorStat(data));
            }

            statsByZoneDay.get(key).add(toSensorStat(data));
        }

        Map<ZoneDay, ZoneDailySummary> summaries = new LinkedHashMap<>();

        statsByZoneDay.forEach((key, stats) -> summaries.put(
                key,
                new ZoneDailySummary(
                        key.zoneId(),
                        sortedBySensorType(stats),
                        doorByZoneDay.get(key)
                )
        ));

        return summaries;
    }

    private Long parseId(
            FluxRecord data,
            String tag
    ) {
        String value = FluxRecords.getString(data, tag);

        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            log.warn("{} 태그가 숫자 형식이 아닙니다. value={}", tag, value);

            return null;
        }
    }

    // 한 구역의 하루를 가리키는 묶음 키.
    private record ZoneDay(
            Long zoneId,
            LocalDate date
    ) {
    }

    private List<SensorDailyStat> sortedBySensorType(List<SensorDailyStat> stats) {
        return stats.stream()
                .sorted(Comparator.comparing(SensorDailyStat::sensorType))
                .toList();
    }

    private SensorDailyStat toSensorStat(FluxRecord data) {
        return new SensorDailyStat(
                FluxRecords.getString(data, SENSOR_TYPE),
                FluxRecords.getString(data, UNIT),
                FluxRecords.requireDouble(data, AVG),
                FluxRecords.requireDouble(data, MIN),
                FluxRecords.requireDouble(data, MAX),
                FluxRecords.getDouble(data, THRESHOLD_MIN),
                FluxRecords.getDouble(data, THRESHOLD_MAX),
                FluxRecords.getDouble(data, OUT_OF_RANGE_RATIO),
                FluxRecords.getDouble(data, PREVIOUS_DAY_AVG)
        );
    }

    private DoorDailyStat toDoorStat(FluxRecord data) {
        return new DoorDailyStat(
                (long) FluxRecords.requireDouble(data, OPEN_COUNT),
                (long) FluxRecords.requireDouble(data, OPEN_MINUTES)
        );
    }

}
