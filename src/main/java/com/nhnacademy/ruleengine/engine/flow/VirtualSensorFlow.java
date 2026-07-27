package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitNormalizedPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.VirtualSensorGeneratorNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;

import java.util.Map;

//가상 센서 데이터를 생성해 내부 MQTT로 전송하는 Flow
public class VirtualSensorFlow {

    public static final String FLOW_ID_PREFIX = "virtual-sensor-flow-";

    static final String SENSOR_GENERATOR_NODE_ID = "sensor-generator";
    static final String RABBIT_NORMALIZED_PUBLISHER_NODE_ID = "rabbit-normalized-publisher";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final String flowId;
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    private final Map<String, Object> sensorConfig;

    public VirtualSensorFlow(
            String sectionId,
            Map<String, Object> sensorConfig,
            NormalizedSensorPublisher normalizedSensorPublisher
    ) {
        this.flowId = FLOW_ID_PREFIX + sectionId;
        this.sensorConfig = sensorConfig;
        this.normalizedSensorPublisher = normalizedSensorPublisher;
    }

    public Flow create() {

        return new Flow(flowId)
                .addNode(new VirtualSensorGeneratorNode(
                        SENSOR_GENERATOR_NODE_ID,
                        sensorConfig
                ))
                .addNode(new RabbitNormalizedPublisherNode(
                        RABBIT_NORMALIZED_PUBLISHER_NODE_ID,
                        normalizedSensorPublisher
                ))
                .connect(
                        SENSOR_GENERATOR_NODE_ID,
                        OUTPUT_PORT,
                        RABBIT_NORMALIZED_PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

}
