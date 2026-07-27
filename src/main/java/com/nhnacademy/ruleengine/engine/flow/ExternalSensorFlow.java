package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.command.SensorCommand;
import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog;
import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.node.impl.MqttPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.MqttSubscriberNode;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitRawPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorTransformNode;
import com.nhnacademy.ruleengine.engine.rabbit.RawSensorPublisher;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
// 외부 MQTT 데이터 전처리후 내부 MQTT로 전송하는 Flow
public class ExternalSensorFlow implements FlowFactory {

    public static final String FLOW_ID = "sensor-processing-flow";

    static final String SUBSCRIBER_NODE_ID = "external-mqtt-in";
    static final String TRANSFORM_NODE_ID = "sensor-filter";
    static final String PUBLISHER_NODE_ID = "internal-mqtt-out";
    static final String RABBIT_RAW_PUBLISHER_NODE_ID = "rabbit-raw-publisher";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final List<SensorCommand> sensorCommands;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final SectionCatalog sectionCatalog;
    private final RawSensorPublisher rawSensorPublisher;


    @Override
    public Flow create() {
        ExternalConfig external = properties.mqtt().external();
        InternalConfig internal = properties.mqtt().internal();

        return new Flow(FLOW_ID)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        mqttNodeConfigFactory.createExternalSubscriberConfig(external)
                ))
                .addNode(new SensorTransformNode(
                        TRANSFORM_NODE_ID,
                        sensorCommands,
                        sectionCatalog
                ))
                .addNode(new RabbitRawPublisherNode(RABBIT_RAW_PUBLISHER_NODE_ID, rawSensorPublisher))
                .addNode(new MqttPublisherNode(
                        PUBLISHER_NODE_ID,
                        mqttNodeConfigFactory.createInternalPublisherConfig(internal)
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
                        PUBLISHER_NODE_ID,
                        INPUT_PORT
                )
                .connect(SUBSCRIBER_NODE_ID,
                        OUTPUT_PORT,
                        RABBIT_RAW_PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

}
