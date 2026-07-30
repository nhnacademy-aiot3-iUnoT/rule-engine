package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HumiditySensorCommandTest {
    private HumiditySensorCommand command = new HumiditySensorCommand();
    private SensorContext sensorContext;

    @BeforeEach
    void setUp() {
        sensorContext = new SensorContext(
                1L,
                "test-device-eui",
                2L,
                3L,
                "test-time"
        );
    }

    @Test
    @DisplayName("센서 데이터를 처리한다")
    void execute() {
        SensorPayload payload = command.execute("25.5", sensorContext);

        assertEquals(25.5, payload.value());
    }

    @Test
    @DisplayName("값이 null일 때 IllegalArgumentException을 던진다")
    void executeNullValue() {
        assertThrows(IllegalArgumentException.class, () -> command.execute(null, sensorContext));

    }

    @Test
    @DisplayName("값이 수가아닐때  NumberFormatException을 던진다")
    void executeNonNumberValue() {
        assertThrows(NumberFormatException.class, () -> command.execute("unknown", sensorContext));
    }

    @Test
    @DisplayName("문 측정 키를 반환한다")
    void getMeasurementKey() {
        assertEquals(SensorType.HUMIDITY.value(), command.getMeasurementKey());
    }

    @Test
    @DisplayName("센서 타입을 반홚나다")
    void getSensorType() {
        assertEquals(SensorType.HUMIDITY, command.getSensorType());
    }
}