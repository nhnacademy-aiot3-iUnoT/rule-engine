package com.nhnacademy.ruleengine.engine.repository.support;

import com.influxdb.query.FluxRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FluxRecordsTest {

    @Test
    @DisplayName("문자열 값을 읽고, 없으면 null을 돌려준다")
    void getString() {
        FluxRecord fluxRecord = recordValues(Map.of("sensor_type", "temperature"));

        assertAll(
                () -> assertEquals("temperature", FluxRecords.getString(fluxRecord, "sensor_type")),
                () -> assertNull(FluxRecords.getString(fluxRecord, "unit"))
        );
    }

    @Test
    @DisplayName("설정되지 않은 열은 null이고, 있으면 숫자로 읽는다")
    void getDouble() {
        FluxRecord fluxRecord = recordValues(Map.of("avg", 21.5));

        assertAll(
                () -> assertEquals(21.5, FluxRecords.getDouble(fluxRecord, "avg")),
                () -> assertNull(FluxRecords.getDouble(fluxRecord, "threshold_min"))
        );
    }

    @Test
    @DisplayName("반드시 있어야 하는 열이 없으면 예외를 던진다")
    void requireDoubleThrowsWhenAbsent() {
        FluxRecord fluxRecord = recordValues(Map.of());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> FluxRecords.requireDouble(fluxRecord, "avg")
        );

        assertTrue(exception.getMessage().contains("avg"));
    }

    @Test
    @DisplayName("숫자 자리에 숫자가 아닌 값이 있으면 즉시 예외를 던진다")
    void throwsOnNonNumeric() {
        FluxRecord fluxRecord = recordValues(Map.of("avg", "많음"));

        assertThrows(
                IllegalStateException.class,
                () -> FluxRecords.getDouble(fluxRecord, "avg")
        );
    }

    @Test
    @DisplayName("_value를 숫자로 읽는다")
    void requireValue() {
        assertEquals(21.5, FluxRecords.requireValue(recordValues(Map.of("_value", 21.5))));
    }

    @Test
    @DisplayName("_value가 없으면 예외를 던진다")
    void requireValueThrowsWhenAbsent() {
        FluxRecord fluxRecord = recordValues(Map.of());

        assertThrows(
                IllegalStateException.class,
                () -> FluxRecords.requireValue(fluxRecord)
        );
    }

    @Test
    @DisplayName("Flux 열 목록 표기를 만든다")
    void toColumns() {
        assertAll(
                () -> assertEquals("[\"zone_id\"]", FluxRecords.toColumns("zone_id")),
                () -> assertEquals(
                        "[\"sensor_type\", \"unit\"]",
                        FluxRecords.toColumns("sensor_type", "unit")
                )
        );
    }

    private FluxRecord recordValues(Map<String, Object> values) {
        FluxRecord fluxRecord = new FluxRecord(0);
        fluxRecord.getValues().putAll(values);

        return fluxRecord;
    }
}
