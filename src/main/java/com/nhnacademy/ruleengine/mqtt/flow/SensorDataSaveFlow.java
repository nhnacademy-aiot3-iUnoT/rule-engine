package com.nhnacademy.ruleengine.mqtt.flow;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties.InternalConfig;
import com.nhnacademy.ruleengine.mqtt.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.mqtt.node.MqttSubscriberNode;
import com.nhnacademy.ruleengine.sensor.node.DatabaseSaveNode;
import com.nhnacademy.ruleengine.sensor.service.SensorInfluxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
// 내부 MQTT에서 받은 데이터를 데이터베이스에 저장하는 Flow
public class SensorDataSaveFlow implements FlowFactory {

    public static final String FLOW_ID = "sensor-data-save-flow";

    static final String SUBSCRIBER_NODE_ID = "internal-mqtt-in";
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
                .addNode(new DatabaseSaveNode(
                        DATABASE_SAVE_NODE_ID,
                        sensorInfluxService
                ))
                .connect(
                        SUBSCRIBER_NODE_ID,
                        OUTPUT_PORT,
                        DATABASE_SAVE_NODE_ID,
                        INPUT_PORT
                );
    }
}
