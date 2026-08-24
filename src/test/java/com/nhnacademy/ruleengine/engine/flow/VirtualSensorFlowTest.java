package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.virtual.GenerationMode;
import com.nhnacademy.ruleengine.engine.dto.virtual.SensorValue;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitNormalizedPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.VirtualSensorGeneratorNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class VirtualSensorFlowTest {

    private static final Long ZONE_ID = 3L;

    @Mock
    private NormalizedSensorPublisher normalizedSensorPublisher;

    private VirtualSensorFlow virtualSensorFlow;

    @BeforeEach
    void setUp() {
        virtualSensorFlow = new VirtualSensorFlow(normalizedSensorPublisher);
    }

    @Test
    @DisplayName("Flow ID는 구역 번호로 만들어 구역마다 구분된다")
    void flowIdIncludesZoneId() {
        assertAll(
                () -> assertEquals("virtual-sensor-flow-3", VirtualSensorFlow.flowId(ZONE_ID)),
                () -> assertEquals(
                        VirtualSensorFlow.flowId(ZONE_ID),
                        virtualSensorFlow.create(config(ZONE_ID)).getId()
                ),
                () -> assertNotEquals(
                        VirtualSensorFlow.flowId(ZONE_ID),
                        virtualSensorFlow.create(config(4L)).getId()
                )
        );
    }

    @Test
    @DisplayName("생성 노드와 발행 노드 두 개로 구성된다")
    void createNodes() {
        Flow flow = virtualSensorFlow.create(config(ZONE_ID));

        assertAll(
                () -> assertEquals(2, flow.getNodes().size()),
                () -> assertInstanceOf(
                        VirtualSensorGeneratorNode.class,
                        flow.getNodes().get(VirtualSensorFlow.SENSOR_GENERATOR_NODE_ID)
                ),
                () -> assertInstanceOf(
                        RabbitNormalizedPublisherNode.class,
                        flow.getNodes().get(VirtualSensorFlow.RABBIT_NORMALIZED_PUBLISHER_NODE_ID)
                )
        );
    }

    @Test
    @DisplayName("생성 → 발행 순서로 연결한다")
    void connectGeneratorToPublisher() {
        Flow flow = virtualSensorFlow.create(config(ZONE_ID));

        assertAll(
                () -> assertEquals(1, flow.getConnections().size()),
                () -> assertNotNull(flow.getConnection(
                        VirtualSensorFlow.SENSOR_GENERATOR_NODE_ID + ":out->"
                                + VirtualSensorFlow.RABBIT_NORMALIZED_PUBLISHER_NODE_ID + ":in"
                ))
        );
    }

    @Test
    @DisplayName("배선에 문제가 없어 검증을 통과한다")
    void validate() {
        assertTrue(virtualSensorFlow.create(config(ZONE_ID)).validate().isEmpty());
    }

    @Test
    @DisplayName("설정이 없으면 Flow를 만들지 않는다")
    void requireConfig() {
        assertThrows(NullPointerException.class, () -> virtualSensorFlow.create(null));
    }

    private VirtualSensorConfig config(Long zoneId) {
        return new VirtualSensorConfig(
                1L,
                2L,
                zoneId,
                "device-eui",
                new VirtualSensorValues(Map.of(
                        SensorType.TEMPERATURE,
                        new SensorValue(GenerationMode.RANGE, 18.0, 26.0, null, null)
                )),
                10L
        );
    }
}
