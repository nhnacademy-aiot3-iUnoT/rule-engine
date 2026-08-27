package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.virtual.GenerationMode;
import com.nhnacademy.ruleengine.engine.dto.virtual.SensorValue;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VirtualSensorGeneratorNodeTest {

    @Test
    @DisplayName("가상 센서 설정이 없으면 생성에 실패한다")
    void requireConfig() {
        assertThrows(
                NullPointerException.class,
                () -> new VirtualSensorGeneratorNode("generator", null)
        );
    }

    @Test
    @DisplayName("FIXED 모드는 설정한 고정값을 그대로 발행한다")
    void generateFixedValue() throws InterruptedException {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.TEMPERATURE, fixed(21.5)
        ));
        LocalConnection connection = connect(node);

        node.process(new Message(Map.of()));

        assertEquals(1, connection.getBufferSize());

        SensorPayload payload = connection.poll().get(MessageFields.SENSOR_PAYLOAD);

        assertAll(
                () -> assertEquals("temperature", payload.sensorType()),
                () -> assertEquals(21.5, payload.value()),
                () -> assertEquals(SensorType.TEMPERATURE.unit(), payload.unit()),
                // 위치는 다음 노드가 deviceEui로 채운다
                () -> assertNull(payload.organizationId()),
                () -> assertNull(payload.storageId()),
                () -> assertNull(payload.zoneId()),
                () -> assertEquals("device eui", payload.deviceEui()),
                () -> assertNotNull(payload.time())
        );
    }

    @Test
    @DisplayName("RANGE 모드는 설정한 최솟값과 최댓값 사이의 값을 발행한다")
    void generateValueInRange() throws InterruptedException {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.HUMIDITY, new SensorValue(GenerationMode.RANGE, 40.0, 60.0, null, null)
        ));
        LocalConnection connection = connect(node);

        node.process(new Message(Map.of()));

        Double value = connection.poll().<SensorPayload>get(MessageFields.SENSOR_PAYLOAD).value();

        assertTrue(value >= 40.0 && value < 60.0, "생성값이 범위를 벗어났습니다: " + value);
    }

    @Test
    @DisplayName("최솟값과 최댓값이 같으면 그 값을 발행한다")
    void generateValueWhenRangeIsSingleValue() throws InterruptedException {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.HUMIDITY, new SensorValue(GenerationMode.RANGE, 50.0, 50.0, null, null)
        ));
        LocalConnection connection = connect(node);

        node.process(new Message(Map.of()));

        assertEquals(50.0, connection.poll().<SensorPayload>get(MessageFields.SENSOR_PAYLOAD).value());
    }

    @Test
    @DisplayName("설정한 센서만 발행하고 설정하지 않은 센서는 건너뛴다")
    void generateOnlyConfiguredSensors() {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.TEMPERATURE, fixed(21.5),
                SensorType.HUMIDITY, fixed(55.0)
        ));
        LocalConnection connection = connect(node);

        node.process(new Message(Map.of()));

        assertEquals(2, connection.getBufferSize());
    }

    @Test
    @DisplayName("door 센서는 상태가 바뀔 때만 발행한다")
    void publishDoorOnlyWhenChanged() throws InterruptedException {
        // 초기 상태는 닫힘(0.0)이므로, 열림(1.0) 고정 설정에서는 첫 tick만 발행된다.
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.DOOR, fixed(1.0)
        ));
        LocalConnection connection = connect(node);

        node.process(new Message(Map.of()));
        node.process(new Message(Map.of()));
        node.process(new Message(Map.of()));

        assertEquals(1, connection.getBufferSize());
        assertEquals(1.0, connection.poll().<SensorPayload>get(MessageFields.SENSOR_PAYLOAD).value());
    }

    @Test
    @DisplayName("door 센서가 초기 상태와 같으면 발행하지 않는다")
    void doNotPublishUnchangedDoor() {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.DOOR, fixed(0.0)
        ));
        LocalConnection connection = connect(node);

        node.process(new Message(Map.of()));

        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("PROBABILITY 모드는 0 또는 1만 생성한다")
    void generateProbabilityValue() throws InterruptedException {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.DOOR, new SensorValue(GenerationMode.PROBABILITY, null, null, null, 0.5)
        ));
        LocalConnection connection = connect(node);

        List<Double> values = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            node.process(new Message(Map.of()));
        }
        while (connection.getBufferSize() > 0) {
            values.add(connection.poll().<SensorPayload>get(MessageFields.SENSOR_PAYLOAD).value());
        }

        assertTrue(
                values.stream().allMatch(value -> value == 0.0 || value == 1.0),
                "0 또는 1이 아닌 값이 생성되었습니다: " + values
        );
    }

    @Test
    @DisplayName("initialize()를 여러 번 호출해도 스케줄러는 하나만 동작한다")
    void initializeIsIdempotent() {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.TEMPERATURE, fixed(21.5)
        ));

        assertDoesNotThrow(() -> {
            node.initialize();
            node.initialize();
            node.shutdown();
        });
    }

    @Test
    @DisplayName("initialize() 없이 shutdown()해도 예외가 없다")
    void shutdownWithoutInitialize() {
        VirtualSensorGeneratorNode node = createNode(Map.of(
                SensorType.TEMPERATURE, fixed(21.5)
        ));

        assertDoesNotThrow(node::shutdown);
    }

    private SensorValue fixed(double value) {
        return new SensorValue(GenerationMode.FIXED, null, null, value, null);
    }

    private VirtualSensorGeneratorNode createNode(Map<SensorType, SensorValue> valueMap) {
        VirtualSensorConfig config = new VirtualSensorConfig(
                1L,
                "device eui",
                new VirtualSensorValues(valueMap),
                1L
        );

        return new VirtualSensorGeneratorNode("generator", config);
    }

    private LocalConnection connect(VirtualSensorGeneratorNode node) {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        return connection;
    }
}
