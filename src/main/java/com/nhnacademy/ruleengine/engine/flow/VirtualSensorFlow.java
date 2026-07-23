package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.node.impl.MqttPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.VirtualSensorGeneratorNode;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;

import java.util.Map;

//가상 센서 데이터를 생성해 내부 MQTT로 전송하는 Flow
public class VirtualSensorFlow {

    public static final String FLOW_ID_PREFIX = "virtual-sensor-flow-";

    static final String PUBLISHER_NODE_ID = "internal-mqtt-out";
    static final String SENSOR_GENERATOR_NODE_ID = "sensor-generator";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final String flowId;
    private final RuleEngineProperties properties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final Map<String, Object> sensorConfig;

    public VirtualSensorFlow(
            String sectionId,
            RuleEngineProperties properties,
            MqttNodeConfigFactory mqttNodeConfigFactory,
            Map<String, Object> sensorConfig
    ) {
        this.flowId = FLOW_ID_PREFIX + sectionId;
        this.properties = properties;
        this.mqttNodeConfigFactory = mqttNodeConfigFactory;
        this.sensorConfig = sensorConfig;
    }

    public Flow create() {
        InternalConfig internal = properties.mqtt().internal();

        return new Flow(flowId)
                .addNode(new VirtualSensorGeneratorNode(
                        SENSOR_GENERATOR_NODE_ID,
                        sensorConfig
                ))
                .addNode(new MqttPublisherNode(
                        PUBLISHER_NODE_ID,
                        mqttNodeConfigFactory.createInternalPublisherConfig(internal)
                ))
                .connect(
                        SENSOR_GENERATOR_NODE_ID,
                        OUTPUT_PORT,
                        PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

}
