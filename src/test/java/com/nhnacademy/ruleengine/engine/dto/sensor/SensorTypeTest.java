package com.nhnacademy.ruleengine.engine.dto.sensor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SensorTypeTest {

    @Test
    @DisplayName("센서 타입을 값으로 찾는다")
    void findByValue_returnsMatchingSensorType() {
        assertAll(
                () -> assertEquals(
                        SensorType.TEMPERATURE,
                        SensorType.findByValue("temperature").orElseThrow()
                ),
                () -> assertEquals(
                        SensorType.HUMIDITY,
                        SensorType.findByValue("humidity").orElseThrow()
                ),
                () -> assertEquals(
                        SensorType.DOOR,
                        SensorType.findByValue("DOOR").orElseThrow()
                ),
                () -> assertEquals(
                        SensorType.ILLUMINATION,
                        SensorType.findByValue("illumination  ").orElseThrow()
                )
        );
    }

    @Test
    @DisplayName("센서 타입을 단위로 찾는다")
    void findByUnit_returnsMatchingSensorType() {
        assertAll(
                () -> assertEquals("C", SensorType.TEMPERATURE.unit()),
                () -> assertEquals("%", SensorType.HUMIDITY.unit()),
                () -> assertEquals("bool", SensorType.DOOR.unit()),
                () -> assertEquals("lux", SensorType.ILLUMINATION.unit())
        );
    }

    @Test
    @DisplayName("값이 알려지지 않은 경우 빈 Optional을 반환한다")
    void findByValue_returnsEmpty_whenValueIsUnknown() {
        assertTrue(SensorType.findByValue("unknown").isEmpty());
    }
}