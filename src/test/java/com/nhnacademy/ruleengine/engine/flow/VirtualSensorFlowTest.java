package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.virtual.GenerationMode;
import com.nhnacademy.ruleengine.engine.dto.virtual.SensorValue;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitNormalizedPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorZoneResolveNode;
import com.nhnacademy.ruleengine.engine.node.impl.VirtualSensorGeneratorNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import com.nhnacademy.ruleengine.engine.service.ZoneResolver;
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

    private static final String DEVICE_EUI = "device-eui";

    @Mock
    private NormalizedSensorPublisher normalizedSensorPublisher;

    @Mock
    private ZoneResolver zoneResolver;

    private VirtualSensorFlow virtualSensorFlow;

    @BeforeEach
    void setUp() {
        virtualSensorFlow = new VirtualSensorFlow(normalizedSensorPublisher, zoneResolver);
    }

    @Test
    @DisplayName("Flow ID는 deviceEui로 만들어 기기마다 구분된다")
    void flowIdIncludesDeviceEui() {
        assertAll(
                () -> assertEquals("virtual-sensor-flow-device-eui", VirtualSensorFlow.flowId(DEVICE_EUI)),
                () -> assertEquals(
                        VirtualSensorFlow.flowId(DEVICE_EUI),
                        virtualSensorFlow.create(config(DEVICE_EUI)).getId()
                ),
                () -> assertNotEquals(
                        VirtualSensorFlow.flowId(DEVICE_EUI),
                        virtualSensorFlow.create(config("other-device")).getId()
                )
        );
    }

    @Test
    @DisplayName("생성 노드, 구역 해석 노드, 발행 노드 세 개로 구성된다")
    void createNodes() {
        Flow flow = virtualSensorFlow.create(config(DEVICE_EUI));

        assertAll(
                () -> assertEquals(3, flow.getNodes().size()),
                () -> assertInstanceOf(
                        VirtualSensorGeneratorNode.class,
                        flow.getNodes().get(VirtualSensorFlow.SENSOR_GENERATOR_NODE_ID)
                ),
                () -> assertInstanceOf(
                        SensorZoneResolveNode.class,
                        flow.getNodes().get(VirtualSensorFlow.ZONE_RESOLVE_NODE_ID)
                ),
                () -> assertInstanceOf(
                        RabbitNormalizedPublisherNode.class,
                        flow.getNodes().get(VirtualSensorFlow.RABBIT_NORMALIZED_PUBLISHER_NODE_ID)
                )
        );
    }

    @Test
    @DisplayName("생성 → 구역 해석 → 발행 순서로 연결한다")
    void connectGeneratorToPublisher() {
        Flow flow = virtualSensorFlow.create(config(DEVICE_EUI));

        assertAll(
                () -> assertEquals(2, flow.getConnections().size()),
                () -> assertNotNull(flow.getConnection(
                        VirtualSensorFlow.SENSOR_GENERATOR_NODE_ID + ":out->"
                                + VirtualSensorFlow.ZONE_RESOLVE_NODE_ID + ":in"
                )),
                () -> assertNotNull(flow.getConnection(
                        VirtualSensorFlow.ZONE_RESOLVE_NODE_ID + ":out->"
                                + VirtualSensorFlow.RABBIT_NORMALIZED_PUBLISHER_NODE_ID + ":in"
                ))
        );
    }

    @Test
    @DisplayName("배선에 문제가 없어 검증을 통과한다")
    void validate() {
        assertTrue(virtualSensorFlow.create(config(DEVICE_EUI)).validate().isEmpty());
    }

    @Test
    @DisplayName("설정이 없으면 Flow를 만들지 않는다")
    void requireConfig() {
        assertThrows(NullPointerException.class, () -> virtualSensorFlow.create(null));
    }

    private VirtualSensorConfig config(String deviceEui) {
        return new VirtualSensorConfig(
                1L,
                deviceEui,
                new VirtualSensorValues(Map.of(
                        SensorType.TEMPERATURE,
                        new SensorValue(GenerationMode.RANGE, 18.0, 26.0, null, null)
                )),
                10L
        );
    }
}
