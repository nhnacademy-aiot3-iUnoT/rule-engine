package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitNormalizedPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.VirtualSensorGeneratorNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;

import java.util.Objects;

// 가상 센서 데이터를 생성해 정규화 RabbitMQ Queue로 전송하는 Flow
public class VirtualSensorFlow {

    public static final String FLOW_ID_PREFIX = "virtual-sensor-flow-";

    static final String SENSOR_GENERATOR_NODE_ID = "sensor-generator";
    static final String RABBIT_NORMALIZED_PUBLISHER_NODE_ID = "rabbit-normalized-publisher";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final String flowId;
    private final NormalizedSensorPublisher normalizedSensorPublisher;

    private final VirtualSensorConfig sensorConfig;

    public VirtualSensorFlow(
            VirtualSensorConfig sensorConfig,
            NormalizedSensorPublisher normalizedSensorPublisher
    ) {
        this.sensorConfig = Objects.requireNonNull(sensorConfig, "가상 센서 설정은 필수입니다.");
        this.flowId = flowId(this.sensorConfig.sectionId());
        this.normalizedSensorPublisher = normalizedSensorPublisher;
    }

    public static String flowId(Long sectionId) {
        return FLOW_ID_PREFIX + sectionId;
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
