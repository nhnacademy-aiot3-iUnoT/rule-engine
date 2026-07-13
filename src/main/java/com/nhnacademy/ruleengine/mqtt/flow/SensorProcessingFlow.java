package com.nhnacademy.ruleengine.mqtt.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.ExternalConfig;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;
import com.nhnacademy.ruleengine.mqtt.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.mqtt.node.MqttPublisherNode;
import com.nhnacademy.ruleengine.mqtt.node.MqttSubscriberNode;
import com.nhnacademy.ruleengine.sensor.command.SensorCommand;
import com.nhnacademy.ruleengine.sensor.node.SensorFilterTransformNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
// 외부 MQTT 데이터 전처리후 내부 MQTT로 전송하는 Flow
public class SensorProcessingFlow implements FlowFactory {

    public static final String FLOW_ID = "sensor-processing-flow";

    static final String SUBSCRIBER_NODE_ID = "external-mqtt-in";
    static final String TRANSFORM_NODE_ID = "sensor-filter";
    static final String PUBLISHER_NODE_ID = "internal-mqtt-out";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final List<SensorCommand> sensorCommands;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;

    @Override
    public Flow create() {
        ExternalConfig external = properties.mqtt().external();
        InternalConfig internal = properties.mqtt().internal();

        return new Flow(FLOW_ID)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        mqttNodeConfigFactory.createExternalSubscriberConfig(external)
                ))
                .addNode(new SensorFilterTransformNode(
                        TRANSFORM_NODE_ID,
                        sensorCommands
                ))
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
                );
    }

}
