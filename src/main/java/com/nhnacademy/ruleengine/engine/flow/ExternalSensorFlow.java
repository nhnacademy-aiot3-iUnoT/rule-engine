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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 외부 MQTT 데이터를 수집·변환해 정규화 RabbitMQ Queue로 전달하는 Flow
// 변환은 메모리 연산뿐이라 별도 Queue를 거치지 않고 수집 흐름 안에서 처리한다.
@Component
@RequiredArgsConstructor
public class ExternalSensorFlow {

    public static final String FLOW_ID = "sensor-processing-flow";

    static final String SUBSCRIBER_NODE_ID = "external-mqtt-in";
    static final String TRANSFORM_NODE_ID = "sensor-transform";
    static final String RABBIT_NORMALIZED_PUBLISHER_NODE_ID = "rabbit-normalized-publisher";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final SensorTransformService sensorTransformService;
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    public Flow create() {
        ExternalConfig external = properties.mqtt().external();

        return new Flow(FLOW_ID)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        mqttNodeConfigFactory.createExternalSubscriberConfig(external)
                ))
                .addNode(new SensorTransformNode(
                        TRANSFORM_NODE_ID,
                        sensorTransformService
                ))
                .addNode(new RabbitNormalizedPublisherNode(
                        RABBIT_NORMALIZED_PUBLISHER_NODE_ID,
                        normalizedSensorPublisher
                ))
                .connect(
                        SUBSCRIBER_NODE_ID,
                        OUTPUT_PORT,
                        TRANSFORM_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        TRANSFORM_NODE_ID,
                        OUTPUT_PORT,
                        RABBIT_NORMALIZED_PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

}
