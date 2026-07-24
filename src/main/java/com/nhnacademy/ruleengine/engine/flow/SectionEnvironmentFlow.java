package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.node.impl.DatabaseSaveNode;
import com.nhnacademy.ruleengine.engine.node.impl.MqttSubscriberNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorPayloadValidationNode;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;

// 각 section별 환경 플로우 (sectionId)
// 내부 MQTT iot/# 토픽을 구독 + 센서 데이터 검증 + InfluxDB에 저장 + 환경룰
public class SectionEnvironmentFlow {

    private final Long sectionId;

    static final String FLOW_ID_PREFIX = "section-environment-flow-";
    static final String SUBSCRIBER_NODE_ID = "internal-mqtt-subscriber";
    static final String VALIDATION_NODE_ID = "sensor-payload-validation";
    static final String DATABASE_SAVE_NODE_ID = "sensor-database-save";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final RuleEngineProperties properties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final SensorInfluxService sensorInfluxService;

    public SectionEnvironmentFlow(Long sectionId, RuleEngineProperties properties, MqttNodeConfigFactory mqttNodeConfigFactory, SensorInfluxService sensorInfluxService) {
        this.sectionId = sectionId;
        this.properties = properties;
        this.mqttNodeConfigFactory = mqttNodeConfigFactory;
        this.sensorInfluxService = sensorInfluxService;
    }

    public Flow create() {
        InternalConfig internal = properties.mqtt().internal();
        String flowId = flowId(sectionId);
        return new Flow(flowId)
                .addNode(new MqttSubscriberNode(
                        SUBSCRIBER_NODE_ID,
                        mqttNodeConfigFactory.createInternalSubscriberConfig(internal, "+/+/" + sectionId + "/#")
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

    public static String flowId(Long sectionId) {
        return FLOW_ID_PREFIX + sectionId;
    }
}
