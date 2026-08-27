package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.node.impl.RabbitNormalizedPublisherNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorZoneResolveNode;
import com.nhnacademy.ruleengine.engine.node.impl.VirtualSensorGeneratorNode;
import com.nhnacademy.ruleengine.engine.rabbit.NormalizedSensorPublisher;
import com.nhnacademy.ruleengine.engine.service.ZoneResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

// 가상 센서 데이터를 생성해 정규화 RabbitMQ Queue로 전송하는 Flow
// 실제 센서와 같은 경로를 타도록, 구역은 생성 시점이 아니라 데이터마다 deviceEui로 해석한다.
@Component
@RequiredArgsConstructor
public class VirtualSensorFlow {

    public static final String FLOW_ID_PREFIX = "virtual-sensor-flow-";

    static final String SENSOR_GENERATOR_NODE_ID = "sensor-generator";
    static final String ZONE_RESOLVE_NODE_ID = "sensor-zone-resolve";
    static final String RABBIT_NORMALIZED_PUBLISHER_NODE_ID = "rabbit-normalized-publisher";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final NormalizedSensorPublisher normalizedSensorPublisher;
    private final ZoneResolver zoneResolver;

    public static String flowId(String deviceEui) {
        return FLOW_ID_PREFIX + deviceEui;
    }

    public Flow create(VirtualSensorConfig sensorConfig) {
        VirtualSensorConfig config = Objects.requireNonNull(
                sensorConfig,
                "가상 센서 설정은 필수입니다."
        );

        return new Flow(flowId(config.deviceEui()))
                .addNode(new VirtualSensorGeneratorNode(
                        SENSOR_GENERATOR_NODE_ID,
                        config
                ))
                .addNode(new SensorZoneResolveNode(
                        ZONE_RESOLVE_NODE_ID,
                        zoneResolver
                ))
                .addNode(new RabbitNormalizedPublisherNode(
                        RABBIT_NORMALIZED_PUBLISHER_NODE_ID,
                        normalizedSensorPublisher
                ))
                .connect(
                        SENSOR_GENERATOR_NODE_ID,
                        OUTPUT_PORT,
                        ZONE_RESOLVE_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        ZONE_RESOLVE_NODE_ID,
                        OUTPUT_PORT,
                        RABBIT_NORMALIZED_PUBLISHER_NODE_ID,
                        INPUT_PORT
                );
    }

}
