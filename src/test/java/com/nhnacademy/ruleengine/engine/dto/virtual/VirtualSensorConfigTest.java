package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualSensorConfigTest {

    private static final VirtualSensorValues SENSOR_VALUES = new VirtualSensorValues(
            Map.of(
                    SensorType.TEMPERATURE,
                    new SensorValue(GenerationMode.FIXED, null, null, 20.0, null)
            )
    );

    @Test
    @DisplayName("필수 설정이 유효하면 생성된다")
    void createValidConfig() {
        assertDoesNotThrow(() -> new VirtualSensorConfig(
                1L,
                2L,
                3L,
                "virtual-device",
                SENSOR_VALUES,
                1L
        ));
    }

    @Test
    @DisplayName("식별자는 양수여야 한다")
    void rejectNonPositiveId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorConfig(0L, 2L, 3L, "device", SENSOR_VALUES, 1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorConfig(1L, -1L, 3L, "device", SENSOR_VALUES, 1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorConfig(1L, 2L, null, "device", SENSOR_VALUES, 1L)
        );
    }

    @Test
    @DisplayName("deviceEui와 측정 주기는 유효해야 한다")
    void rejectInvalidDeviceOrInterval() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorConfig(1L, 2L, 3L, " ", SENSOR_VALUES, 1L)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorConfig(1L, 2L, 3L, "device", SENSOR_VALUES, 0L)
        );
    }
}
