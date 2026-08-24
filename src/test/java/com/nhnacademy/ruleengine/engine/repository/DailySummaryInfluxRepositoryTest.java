package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.environment.DoorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.SensorDailyStat;
import com.nhnacademy.ruleengine.engine.dto.environment.StorageDailySummary;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.exception.SensorDataSaveException;
import com.nhnacademy.ruleengine.engine.repository.support.FluxQueryExecutor;
import com.nhnacademy.ruleengine.global.config.InfluxDbProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DailySummaryInfluxRepositoryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 8, 24);
    private static final Instant START_OF_DAY = ZoneDailySummary.startOfDay(DATE);

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private WriteApiBlocking writeApiBlocking;

    @Mock
    private FluxQueryExecutor fluxQueryExecutor;

    private DailySummaryInfluxRepository repository;

    @BeforeEach
    void setUp() {
        InfluxDbProperties properties = new InfluxDbProperties(
                "http://localhost:8086", "token", "org", "bucket", "sensor_data", "summary-bucket"
        );

        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApiBlocking);

        repository = new DailySummaryInfluxRepository(influxDBClient, properties, fluxQueryExecutor);
    }

    @Test
    @DisplayName("수집된 데이터가 없으면 아무것도 저장하지 않는다")
    void skipSaveWhenNoData() {
        repository.save(new StorageDailySummary(2L, DATE, List.of()));

        verify(writeApiBlocking, never()).writePoints(any(), any(), anyList());
    }

    @Test
    @DisplayName("센서 통계와 문 통계를 각각 포인트로 저장한다")
    void saveSensorAndDoorPoints() {
        repository.save(new StorageDailySummary(2L, DATE, List.of(
                new ZoneDailySummary(3L, List.of(sensorStat()), new DoorDailyStat(5L, 42L))
        )));

        List<Point> points = capturePoints();

        assertEquals(2, points.size());

        String sensorLine = points.get(0).toLineProtocol();
        String doorLine = points.get(1).toLineProtocol();

        assertAll(
                () -> assertTrue(sensorLine.startsWith("zone_daily_summary,"), sensorLine),
                () -> assertTrue(sensorLine.contains("storage_id=2"), sensorLine),
                () -> assertTrue(sensorLine.contains("zone_id=3"), sensorLine),
                () -> assertTrue(sensorLine.contains("sensor_type=temperature"), sensorLine),
                () -> assertTrue(sensorLine.contains("avg=21.5"), sensorLine),
                () -> assertTrue(doorLine.contains("sensor_type=door"), doorLine),
                () -> assertTrue(doorLine.contains("open_count=5"), doorLine),
                () -> assertTrue(doorLine.contains("open_minutes=42"), doorLine)
        );
    }

    @Test
    @DisplayName("값이 없는 항목은 필드를 만들지 않는다")
    void skipNullFields() {
        repository.save(new StorageDailySummary(2L, DATE, List.of(
                new ZoneDailySummary(
                        3L,
                        List.of(new SensorDailyStat(
                                "temperature", "C", 21.5, 18.0, 26.0,
                                null, null, null, null
                        )),
                        null
                )
        )));

        String lineProtocol = capturePoints().get(0).toLineProtocol();

        assertAll(
                () -> assertEquals(1, capturePoints().size()),
                () -> assertFalse(lineProtocol.contains("threshold_min"), lineProtocol),
                () -> assertFalse(lineProtocol.contains("threshold_max"), lineProtocol),
                () -> assertFalse(lineProtocol.contains("out_of_range_ratio"), lineProtocol),
                () -> assertFalse(lineProtocol.contains("previous_day_avg"), lineProtocol)
        );
    }

    @Test
    @DisplayName("문 통계가 없으면 문 포인트를 만들지 않는다")
    void skipDoorPointWhenAbsent() {
        repository.save(new StorageDailySummary(2L, DATE, List.of(
                new ZoneDailySummary(3L, List.of(sensorStat()), null)
        )));

        List<Point> points = capturePoints();

        assertAll(
                () -> assertEquals(1, points.size()),
                () -> assertFalse(points.get(0).toLineProtocol().contains("sensor_type=door"))
        );
    }

    @Test
    @DisplayName("저장에 실패하면 도메인 예외로 감싼다")
    void saveWrapsFailure() {
        doThrow(new RuntimeException("influx down"))
                .when(writeApiBlocking).writePoints(any(), any(), anyList());

        StorageDailySummary summary = new StorageDailySummary(2L, DATE, List.of(
                new ZoneDailySummary(3L, List.of(sensorStat()), null)
        ));

        assertThrows(SensorDataSaveException.class, () -> repository.save(summary));
    }

    @Test
    @DisplayName("조회 결과를 날짜별 저장소 요약으로 묶는다")
    void findByStorageBetween() {
        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of(
                sensorRecord(3L, "temperature", START_OF_DAY),
                sensorRecord(4L, "humidity", START_OF_DAY)
        ));

        List<StorageDailySummary> summaries = repository.findByStorageBetween(2L, DATE, DATE);

        assertAll(
                () -> assertEquals(1, summaries.size()),
                () -> assertEquals(2L, summaries.get(0).storageId()),
                () -> assertEquals(DATE, summaries.get(0).date()),
                () -> assertEquals(2, summaries.get(0).zones().size())
        );
    }

    @Test
    @DisplayName("같은 구역의 센서 통계는 센서 타입 순으로 정렬한다")
    void sortSensorStatsBySensorType() {
        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of(
                sensorRecord(3L, "temperature", START_OF_DAY),
                sensorRecord(3L, "humidity", START_OF_DAY)
        ));

        List<SensorDailyStat> stats =
                repository.findByStorageBetween(2L, DATE, DATE).get(0).zones().get(0).sensorStats();

        assertAll(
                () -> assertEquals("humidity", stats.get(0).sensorType()),
                () -> assertEquals("temperature", stats.get(1).sensorType())
        );
    }

    @Test
    @DisplayName("문 포인트만 있는 구역도 요약이 만들어진다")
    void zoneWithOnlyDoorRecord() {
        FluxRecord doorRecord = valueRecord(Map.of(
                "zone_id", "3",
                "sensor_type", "door",
                "open_count", 5.0,
                "open_minutes", 42.0
        ), START_OF_DAY);

        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of(doorRecord));

        ZoneDailySummary zone = repository.findByStorageBetween(2L, DATE, DATE).get(0).zones().get(0);

        assertAll(
                () -> assertTrue(zone.sensorStats().isEmpty()),
                () -> assertEquals(5L, zone.door().openCount()),
                () -> assertEquals(42L, zone.door().openMinutes())
        );
    }

    @Test
    @DisplayName("측정 시간이 없는 레코드는 건너뛴다")
    void skipRecordWithoutTime() {
        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of(
                valueRecord(Map.of("zone_id", "3", "sensor_type", "temperature"), null)
        ));

        assertTrue(repository.findByStorageBetween(2L, DATE, DATE).isEmpty());
    }

    @Test
    @DisplayName("구역 하나의 요약을 찾는다")
    void findByZoneAndDate() {
        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of(
                sensorRecord(3L, "temperature", START_OF_DAY)
        ));

        Optional<ZoneDailySummary> zone = repository.findByZoneAndDate(3L, DATE);

        assertAll(
                () -> assertTrue(zone.isPresent()),
                () -> assertEquals(3L, zone.get().zoneId()),
                () -> assertEquals(21.5, zone.get().sensorStats().get(0).avg())
        );
    }

    @Test
    @DisplayName("저장된 요약이 없으면 빈 Optional을 돌려준다")
    void findByZoneAndDateReturnsEmpty() {
        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of());

        assertTrue(repository.findByZoneAndDate(3L, DATE).isEmpty());
    }

    @Test
    @DisplayName("조회 구간의 끝은 마지막 날을 포함하도록 다음 날 자정으로 잡는다")
    void queryRangeIncludesLastDate() {
        when(fluxQueryExecutor.query(anyString())).thenReturn(List.of());

        repository.findByStorageBetween(2L, DATE, DATE);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(fluxQueryExecutor).query(captor.capture());

        String fluxQuery = captor.getValue();

        assertAll(
                () -> assertTrue(fluxQuery.contains(START_OF_DAY.toString()), fluxQuery),
                () -> assertTrue(
                        fluxQuery.contains(ZoneDailySummary.startOfDay(DATE.plusDays(1)).toString()),
                        fluxQuery
                ),
                () -> assertTrue(fluxQuery.contains("summary-bucket"), fluxQuery)
        );
    }

    private List<Point> capturePoints() {
        ArgumentCaptor<List<Point>> captor = ArgumentCaptor.forClass(List.class);
        verify(writeApiBlocking, atLeastOnce())
                .writePoints(eq("summary-bucket"), eq("org"), captor.capture());

        return captor.getValue();
    }

    private SensorDailyStat sensorStat() {
        return new SensorDailyStat(
                "temperature", "C", 21.5, 18.0, 26.0, 18.0, 26.0, 0.07, 20.9
        );
    }

    private FluxRecord sensorRecord(Long zoneId, String sensorType, Instant time) {
        return valueRecord(new LinkedHashMap<>(Map.of(
                "zone_id", String.valueOf(zoneId),
                "sensor_type", sensorType,
                "unit", "C",
                "avg", 21.5,
                "min", 18.0,
                "max", 26.0
        )), time);
    }

    private FluxRecord valueRecord(Map<String, Object> values, Instant time) {
        FluxRecord fluxRecord = new FluxRecord(0);
        fluxRecord.getValues().putAll(new LinkedHashMap<>(values));

        if (time != null) {
            fluxRecord.getValues().put("_time", time);
        }

        return fluxRecord;
    }
}
