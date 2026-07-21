package com.nhnacademy.ruleengine.engine;

/**
 * 노드 사이에서 전달하는 메시지 필드 이름을 정의한다.
 */
public final class MessageFields {

    public static final String EXTERNAL_SENSOR_MESSAGE = "externalSensorMessage";
    public static final String MQTT_RECEIVED_AT = "mqttReceivedAt";
    public static final String SENSOR_PAYLOAD = "sensorPayload";
    public static final String TOPIC = "topic";

    private MessageFields() {
    }
}
