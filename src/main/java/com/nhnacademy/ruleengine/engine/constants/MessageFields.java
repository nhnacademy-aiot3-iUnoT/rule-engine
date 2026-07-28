package com.nhnacademy.ruleengine.engine.constants;

/**
 * 노드 사이에서 전달하는 메시지 필드 이름을 정의한다.
 */
public final class MessageFields {

    public static final String EXTERNAL_SENSOR_MESSAGE = "externalSensorMessage";
    public static final String MQTT_RECEIVED_AT = "mqttReceivedAt";
    public static final String SENSOR_PAYLOAD = "sensorPayload";
    public static final String RULE_RESULT = "ruleResult";
    public static final String TOPIC = "topic";
    public static final String QOS = "qos";
    public static final String CLIENT_ID = "clientId";
    public static final String BROKER_URL = "brokerUrl";
    public static final String FLOW_PROCESSING_COMPLETION = "flowProcessingCompletion";

    private MessageFields() {
    }
}
