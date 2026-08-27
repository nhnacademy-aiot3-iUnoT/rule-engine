package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.node.impl.MqttSubscriberNode;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitNormalizedPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorTransformNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import com.nhnacademy.ruleengine.engine.service.SensorTransformService;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ExternalSensorFlowTest {

    @Mock
    private SensorTransformService sensorTransformService;

    @Mock
    private NormalizedSensorPublisher normalizedSensorPublisher;

    private ExternalSensorFlow externalSensorFlow;

    @BeforeEach
    void setUp() {
        RuleEngineProperties properties = new RuleEngineProperties(
                new RuleEngineProperties.Mqtt(new ExternalConfig(
                        "tcp://localhost:1883",
                        "rule-engine",
                        "application/+/device/+/event/up",
                        1
                ))
        );

        externalSensorFlow = new ExternalSensorFlow(
                properties,
                new MqttNodeConfigFactory(),
                sensorTransformService,
                normalizedSensorPublisher
        );
    }

    @Test
    @DisplayName("수집·변환·발행 노드 세 개로 구성된다")
    void createNodes() {
        Flow flow = externalSensorFlow.create();

        assertAll(
                () -> assertEquals(ExternalSensorFlow.FLOW_ID, flow.getId()),
                () -> assertEquals(3, flow.getNodes().size()),
                () -> assertInstanceOf(
                        MqttSubscriberNode.class,
                        flow.getNodes().get(ExternalSensorFlow.SUBSCRIBER_NODE_ID)
                ),
                () -> assertInstanceOf(
                        SensorTransformNode.class,
                        flow.getNodes().get(ExternalSensorFlow.TRANSFORM_NODE_ID)
                ),
                () -> assertInstanceOf(
                        RabbitNormalizedPublisherNode.class,
                        flow.getNodes().get(ExternalSensorFlow.RABBIT_NORMALIZED_PUBLISHER_NODE_ID)
                )
        );
    }

    @Test
    @DisplayName("수집 → 변환 → 발행 순서로 연결한다")
    void connectInOrder() {
        Flow flow = externalSensorFlow.create();

        assertAll(
                () -> assertEquals(2, flow.getConnections().size()),
                () -> assertNotNull(flow.getConnection(
                        ExternalSensorFlow.SUBSCRIBER_NODE_ID + ":out->"
                                + ExternalSensorFlow.TRANSFORM_NODE_ID + ":in"
                )),
                () -> assertNotNull(flow.getConnection(
                        ExternalSensorFlow.TRANSFORM_NODE_ID + ":out->"
                                + ExternalSensorFlow.RABBIT_NORMALIZED_PUBLISHER_NODE_ID + ":in"
                ))
        );
    }

    @Test
    @DisplayName("배선에 문제가 없어 검증을 통과한다")
    void validate() {
        assertTrue(externalSensorFlow.create().validate().isEmpty());
    }

    @Test
    @DisplayName("호출할 때마다 새 Flow를 만든다")
    void createReturnsNewFlow() {
        assertNotSame(externalSensorFlow.create(), externalSensorFlow.create());
    }

    @Test
    @DisplayName("MQTT 설정이 없으면 Flow를 만들지 않는다")
    void failWhenMqttConfigMissing() {
        ExternalSensorFlow flowWithoutConfig = new ExternalSensorFlow(
                new RuleEngineProperties(new RuleEngineProperties.Mqtt(null)),
                new MqttNodeConfigFactory(),
                sensorTransformService,
                normalizedSensorPublisher
        );

        assertThrows(IllegalArgumentException.class, flowWithoutConfig::create);
    }
}
