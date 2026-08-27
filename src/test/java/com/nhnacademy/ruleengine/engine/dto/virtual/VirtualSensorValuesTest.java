package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualSensorValuesTest {

    @Test
    @DisplayName("센서 설정은 하나 이상이어야 한다")
    void rejectEmptyValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorValues(Map.of())
        );
    }

    @Test
    @DisplayName("문 이외의 센서는 확률 모드를 사용할 수 없다")
    void rejectProbabilityForNumericSensor() {
        SensorValue probability = new SensorValue(
                GenerationMode.PROBABILITY,
                null,
                null,
                null,
                0.5
        );

        Map<SensorType, SensorValue> values = new EnumMap<>(SensorType.class);
        values.put(SensorType.TEMPERATURE, probability);

        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorValues(values)
        );
    }

    @Test
    @DisplayName("문 센서는 범위 모드를 사용할 수 없다")
    void rejectRangeForDoorSensor() {
        SensorValue range = new SensorValue(
                GenerationMode.RANGE,
                0.0,
                1.0,
                null,
                null
        );

        Map<SensorType, SensorValue> valueMap = new EnumMap<>(SensorType.class);
        valueMap.put(SensorType.DOOR, range);

        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorValues(valueMap)
        );
    }

    @Test
    @DisplayName("문 센서 고정값은 0 또는 1이어야 한다")
    void rejectNonBinaryDoorFixedValue() {
        SensorValue fixed = new SensorValue(
                GenerationMode.FIXED,
                null,
                null,
                0.5,
                null
        );

        Map<SensorType, SensorValue> valueMap = new EnumMap<>(SensorType.class);
        valueMap.put(SensorType.DOOR, fixed);

        assertThrows(
                IllegalArgumentException.class,
                () -> new VirtualSensorValues(valueMap)
        );
    }

    @Test
    @DisplayName("생성 후 원본 Map을 변경해도 센서 설정은 바뀌지 않는다")
    void copyValuesDefensively() {
        SensorValue fixed = new SensorValue(
                GenerationMode.FIXED,
                null,
                null,
                20.0,
                null
        );
        EnumMap<SensorType, SensorValue> source = new EnumMap<>(SensorType.class);
        source.put(SensorType.TEMPERATURE, fixed);

        VirtualSensorValues values = new VirtualSensorValues(source);
        source.clear();

        assertEquals(Map.of(SensorType.TEMPERATURE, fixed), values.valueMap());

        Map<SensorType, SensorValue> sensorValueMap = values.valueMap();

        assertThrows(
                UnsupportedOperationException.class,
                sensorValueMap::clear
        );
    }
}
