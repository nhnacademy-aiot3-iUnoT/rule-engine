package com.nhnacademy.ruleengine.engine.dto.sensor;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalSensorMessageTest {

    private Map<String, Object> payload;

    @BeforeEach
    void setUp() {
        payload = Map.of(
                MessageFields.TOPIC, "test-topic",
                MessageFields.MQTT_RECEIVED_AT, 1L,
                "time", "test-time",
                "deviceInfo", Map.of(
                        "applicationName", "test-app",
                        "devEui", "test-devEui",
                        "tags", Map.of(
                                "location", "test-location",
                                "point", "test-point"
                        )
                ),
                "object", Map.of(
                        "temperature", 20.5,
                        "humidity", 60.0
                )
        );
    }

    @Test
    @DisplayName("외부 MQTT payload를 센서 메시지로 변환한다")
    void convertExternalSensorMessage() {
        ExternalSensorMessage result =
                ExternalSensorMessage.from(payload);

        assertAll(
                () -> assertEquals("test-time", result.time()),
                () -> assertEquals("test-topic", result.topic()),
                () -> assertEquals(1L, result.receivedAt()),
                () -> assertEquals("test-app", result.applicationName()),
                () -> assertEquals("test-devEui", result.devEui()),
                () -> assertEquals("test-location", result.location()),
                () -> assertEquals("test-point", result.point()),
                () -> assertEquals(20.5, result.measurements().get("temperature")),
                () -> assertEquals(60.0, result.measurements().get("humidity"))
        );
    }

    @Test
    @DisplayName("receivedAt이 문자열이어도 Long으로 변환한다")
    void convertStringReceivedAtToLong() {
        Map<String, Object> stringReceivedAtPayload =
                new java.util.HashMap<>(payload);

        stringReceivedAtPayload.put(
                MessageFields.MQTT_RECEIVED_AT,
                "1000"
        );

        ExternalSensorMessage result =
                ExternalSensorMessage.from(stringReceivedAtPayload);

        assertEquals(1000L, result.receivedAt());
    }

    @Test
    @DisplayName("deviceInfo가 없으면 관련 필드는 null로 변환한다")
    void returnNullWhenDeviceInfoIsMissing() {
        Map<String, Object> payloadWithoutDeviceInfo = Map.of(
                MessageFields.TOPIC, "test-topic",
                "object", Map.of()
        );

        ExternalSensorMessage result =
                ExternalSensorMessage.from(payloadWithoutDeviceInfo);

        assertAll(
                () -> assertEquals("test-topic", result.topic()),
                () -> assertEquals(null, result.applicationName()),
                () -> assertEquals(null, result.devEui()),
                () -> assertEquals(null, result.location()),
                () -> assertEquals(null, result.point()),
                () -> assertTrue(result.measurements().isEmpty())
        );
    }

    @Test
    @DisplayName("receivedAt이 숫자로 변환할 수 없는 문자열이면 예외가 발생한다")
    void throwExceptionWhenReceivedAtIsInvalid() {
        Map<String, Object> invalidPayload =
                new java.util.HashMap<>(payload);

        invalidPayload.put(
                MessageFields.MQTT_RECEIVED_AT,
                "invalid"
        );

        assertThrows(
                NumberFormatException.class,
                () -> ExternalSensorMessage.from(invalidPayload)
        );
    }
}