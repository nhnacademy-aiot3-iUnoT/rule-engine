package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.node.impl.DatabaseSaveNode;
import com.nhnacademy.ruleengine.engine.node.impl.MqttSubscriberNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorPayloadValidationNode;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 내부 MQTT iot/# 토픽을 구독하고 검증한 센서 데이터를 InfluxDB에 저장하는 Flow
public class InternalMqttSubscriberFlow implements FlowFactory {

    public static final String FLOW_ID = "internal-mqtt-subscriber-flow";

    static final String SUBSCRIBER_NODE_ID = "internal-mqtt-subscriber";
    static final String VALIDATION_NODE_ID = "sensor-payload-validation";
    static final String DATABASE_SAVE_NODE_ID = "sensor-database-save";

    private static final String ALL_TOPICS = "#";
    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final SensorInfluxService sensorInfluxService;

    @Override
    public Flow create() {
        InternalConfig internal = properties.mqtt().internal();

        return new Flow(FLOW_ID)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        mqttNodeConfigFactory.createInternalSubscriberConfig(internal, ALL_TOPICS)
                ))
                .addNode(new SensorPayloadValidationNode(
                        VALIDATION_NODE_ID
                ))
                .addNode(new DatabaseSaveNode(
                        DATABASE_SAVE_NODE_ID,
                        sensorInfluxService
                ))
                .connect(
                        SUBSCRIBER_NODE_ID,
                        OUTPUT_PORT,
                        VALIDATION_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        VALIDATION_NODE_ID,
                        OUTPUT_PORT,
                        DATABASE_SAVE_NODE_ID,
                        INPUT_PORT
                );
    }
}
