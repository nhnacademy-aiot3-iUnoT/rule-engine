package com.nhnacademy.ruleengine.engine.repository;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorDataWriteCommand;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorDailyAggregate;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SensorInfluxRepositoryTest {

    private static final Instant TIME = Instant.parse("2026-08-24T00:00:00Z");

    @Mock
    private InfluxDBClient influxDBClient;

    @Mock
    private WriteApiBlocking writeApiBlocking;

    @Mock
    private FluxQueryExecutor fluxQueryExecutor;

    private SensorInfluxRepository repository;

    @BeforeEach
    void setUp() {
        InfluxDbProperties properties = new InfluxDbProperties(
                "http://localhost:8086", "token", "org", "bucket", "sensor_data", "summary-bucket"
        );

        when(influxDBClient.getWriteApiBlocking()).thenReturn(writeApiBlocking);

        repository = new SensorInfluxRepository(influxDBClient, properties, fluxQueryExecutor);
    }

    @Test
    @DisplayName("센서 데이터를 태그와 값으로 나눠 저장한다")
    void save() {
        repository.save(new SensorDataWriteCommand(
                1L, "device-eui", 2L, 3L, "temperature", 21.5, "C", TIME
        ));

        ArgumentCaptor<Point> captor = ArgumentCaptor.forClass(Point.class);
        verify(writeApiBlocking).writePoint(eq("bucket"), eq("org"), captor.capture());

        String lineProtocol = captor.getValue().toLineProtocol();

        assertAll(
                () -> assertTrue(lineProtocol.startsWith("sensor_data,"), lineProtocol),
                () -> assertTrue(lineProtocol.contains("organization_id=1"), lineProtocol),
                () -> assertTrue(lineProtocol.contains("storage_id=2"), lineProtocol),
                () -> assertTrue(lineProtocol.contains("zone_id=3"), lineProtocol),
                () -> assertTrue(lineProtocol.contains("sensor_type=temperature"), lineProtocol),
                () -> assertTrue(lineProtocol.contains("device_eui=device-eui"), lineProtocol),
                () -> assertTrue(lineProtocol.contains("value=21.5"), lineProtocol)
        );
    }

    @Test
    @DisplayName("저장에 실패하면 도메인 예외로 감싼다")
    void saveWrapsFailure() {
        doThrow(new RuntimeException("influx down"))
                .when(writeApiBlocking).writePoint(any(), any(), any(Point.class));

        SensorDataWriteCommand command = new SensorDataWriteCommand(
                1L, "device-eui", 2L, 3L, "temperature", 21.5, "C", TIME
        );

        SensorDataSaveException exception = assertThrows(
                SensorDataSaveException.class,
                () -> repository.save(command)
        );

        assertNotNull(exception.getCause());
    }

    @Test
    @DisplayName("조회 결과를 센서 데이터로 변환한다")
    void findLatestByZone() {
        givenRecords(recordValues(Map.of(
                "organization_id", "1",
                "storage_id", "2",
                "zone_id", "3",
                "device_eui", "device-eui",
                "sensor_type", "temperature",
                "_value", 21.5
        )));

        SensorPayload payload = repository.findLatestByZone(3L).get(0);

        assertAll(
                () -> assertEquals(1L, payload.organizationId()),
                () -> assertEquals(2L, payload.storageId()),
                () -> assertEquals(3L, payload.zoneId()),
                () -> assertEquals("device-eui", payload.deviceEui()),
                () -> assertEquals("temperature", payload.sensorType()),
                () -> assertEquals(21.5, payload.value()),
                () -> assertEquals("C", payload.unit())
        );
    }

    @Test
    @DisplayName("알 수 없는 센서 타입은 저장된 unit 태그를 그대로 쓴다")
    void resolveUnitFallsBackToTag() {
        givenRecords(recordValues(Map.of(
                "zone_id", "3",
                "sensor_type", "pressure",
                "unit", "hPa",
                "_value", 1013.0
        )));

        assertEquals("hPa", repository.findLatestByZone(3L).get(0).unit());
    }

    @Test
    @DisplayName("숫자가 아닌 태그는 조회 자체를 실패시킨다")
    void parseIdThrowsOnNonNumericTag() {
        givenRecords(recordValues(Map.of(
                "organization_id", "not-a-number",
                "zone_id", "3",
                "sensor_type", "temperature",
                "_value", 21.5
        )));

        assertThrows(IllegalStateException.class, () -> repository.findLatestByZone(3L));
    }

    @Test
    @DisplayName("이력 조회 값은 소수점 첫째 자리로 반올림한다")
    void historyValueIsRounded() {
        givenRecords(recordWithTime(Map.of(
                "sensor_type", "temperature",
                "_value", 21.4567
        )));

        SensorHistoryResponse history =
                repository.findHistoryByZone(3L, "temperature", TIME, TIME, "1h").get(0);

        assertAll(
                () -> assertEquals(21.5, history.value()),
                () -> assertEquals("temperature", history.sensorType()),
                () -> assertEquals(TIME, history.time())
        );
    }

    @Test
    @DisplayName("측정 시간이 없는 이력 레코드는 예외를 던진다")
    void historyRequiresTime() {
        givenRecords(recordValues(Map.of(
                "sensor_type", "temperature",
                "_value", 21.5
        )));

        assertThrows(
                IllegalStateException.class,
                () -> repository.findHistoryByZone(3L, "temperature", TIME, TIME, "1h")
        );
    }

    @Test
    @DisplayName("센서 타입을 지정하지 않으면 집계 조회와 door 원본 조회를 합친다")
    void historyWithoutSensorTypeMergesDoor() {
        when(fluxQueryExecutor.query(anyString(), any()))
                .thenAnswer(invocation -> {
                    String query = invocation.getArgument(0);
                    Function<FluxRecord, Object> mapper = invocation.getArgument(1);
                    String sensorType = query.contains("aggregateWindow") ? "temperature" : "door";

                    return List.of(mapper.apply(recordWithTime(Map.of(
                            "sensor_type", sensorType,
                            "_value", 1.0
                    ))));
                });

        List<SensorHistoryResponse> history = repository.findHistoryByZone(3L, null, TIME, TIME, "1h");

        assertAll(
                () -> assertEquals(2, history.size()),
                () -> assertEquals("temperature", history.get(0).sensorType()),
                () -> assertEquals("door", history.get(1).sensorType())
        );
    }

    @Test
    @DisplayName("일별 집계 결과를 변환하고 반올림한다")
    void findDailyAggregatesByZone() {
        givenRecords(recordValues(Map.of(
                "sensor_type", "temperature",
                "count", 100.0,
                "avg", 21.4567,
                "min", 18.44,
                "max", 26.45
        )));

        SensorDailyAggregate aggregate = repository.findDailyAggregatesByZone(3L, TIME, TIME).get(0);

        assertAll(
                () -> assertEquals(100L, aggregate.count()),
                () -> assertEquals(21.5, aggregate.avg()),
                () -> assertEquals(18.4, aggregate.min()),
                () -> assertEquals(26.5, aggregate.max()),
                () -> assertEquals("C", aggregate.unit())
        );
    }

    @Test
    @DisplayName("저장소 번호는 숫자가 아닌 값을 건너뛰고 정렬해서 돌려준다")
    void findStorageIdsSkipsNonNumeric() {
        givenRecords(
                recordWithValue("7"),
                recordWithValue("not-a-number"),
                recordWithValue("2")
        );

        assertEquals(List.of(2L, 7L), repository.findStorageIds(TIME, TIME));
    }

    @Test
    @DisplayName("구역 번호 조회는 저장소 조건을 함께 건다")
    void findZoneIdsByStorage() {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        givenRecords(recordWithValue("3"));

        assertEquals(List.of(3L), repository.findZoneIdsByStorage(2L, TIME, TIME));

        verify(fluxQueryExecutor).query(captor.capture(), any());
        assertTrue(captor.getValue().contains("storage_id"), captor.getValue());
    }

    // FluxQueryExecutor가 넘겨받은 매퍼를 실제로 적용하게 해서 변환 로직만 검증한다.
    private void givenRecords(FluxRecord... records) {
        when(fluxQueryExecutor.query(anyString(), any()))
                .thenAnswer(invocation -> {
                    Function<FluxRecord, Object> mapper = invocation.getArgument(1);

                    return List.of(records).stream().map(mapper).toList();
                });
    }

    private FluxRecord recordValues(Map<String, Object> values) {
        FluxRecord fluxRecord = new FluxRecord(0);
        fluxRecord.getValues().putAll(new LinkedHashMap<>(values));

        return fluxRecord;
    }

    private FluxRecord recordWithTime(Map<String, Object> values) {
        FluxRecord fluxRecord = recordValues(values);
        fluxRecord.getValues().put("_time", TIME);

        return fluxRecord;
    }

    private FluxRecord recordWithValue(Object value) {
        return recordValues(Map.of("_value", value));
    }
}
