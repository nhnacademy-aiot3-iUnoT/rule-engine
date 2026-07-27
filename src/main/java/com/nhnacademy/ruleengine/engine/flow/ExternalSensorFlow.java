package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.node.impl.MqttSubscriberNode;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitRawPublisherNode;
import com.nhnacademy.ruleengine.engine.rabbit.RawSensorPublisher;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 외부 MQTT 데이터 전처리후 내부 MQTT로 전송하는 Flow
public class ExternalSensorFlow {

    public static final String FLOW_ID = "sensor-processing-flow";

    static final String SUBSCRIBER_NODE_ID = "external-mqtt-in";

    static final String RABBIT_RAW_PUBLISHER_NODE_ID = "rabbit-raw-publisher";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final RawSensorPublisher rawSensorPublisher;

    public Flow create() {
        ExternalConfig external = properties.mqtt().external();

        return new Flow(FLOW_ID)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        mqttNodeConfigFactory.createExternalSubscriberConfig(external)
                ))
                .addNode(new RabbitRawPublisherNode(
                        RABBIT_RAW_PUBLISHER_NODE_ID,
                        rawSensorPublisher
                ))
                .connect(
                        SUBSCRIBER_NODE_ID,
                        OUTPUT_PORT,
                        RABBIT_RAW_PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

}
