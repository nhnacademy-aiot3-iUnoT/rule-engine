package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.testsupport.CapturingConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DoorStateFilterNodeTest {
    private DoorStateFilterNode node = new DoorStateFilterNode("door");
    private CapturingConnection connection = new CapturingConnection();
    private SensorPayload sensorPayload;

    private RuleResultDto ruleResult;

    @BeforeEach
    void setUp(){
        node.getOutputPort("out").connect(connection);
    }

    @Test
    void door_값이_1이면_OPEN_위반_결과를_보낸다(){
        sensorPayload = new SensorPayload(1L, "device-1", 2L, 3L,
                "door", 1.0, null, "2026-08-03T10:00:00");

        node.process(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload)));
        ruleResult = connection.captured().get(MessageFields.RULE_RESULT);

        assertEquals(ruleResult.violationType(), ViolationType.OPEN);
        assertTrue(ruleResult.violated());
        assertEquals(ruleResult.value(), 1.0);
        assertNull(ruleResult.durationMinutes());
    }

    @Test
    void door_값이_0이면_CLOSE__정상_결과를_보낸다(){
        sensorPayload = new SensorPayload(1L, "device-1", 2L, 3L,
                "door", 0.0, null, "2026-08-03T10:00:00");

        node.process(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload)));
        ruleResult = connection.captured().get(MessageFields.RULE_RESULT);

        assertEquals(ruleResult.violationType(), ViolationType.CLOSED);
        assertFalse(ruleResult.violated());
        assertEquals(ruleResult.value(), 0.0);
        assertNull(ruleResult.durationMinutes());
    }

    @Test
    void 메세지_보내지_않는_경우(){
        sensorPayload = new SensorPayload(1L, "device-1", 2L, 3L,
                "temperature", 1.0, null, "2026-08-03T10:00:00");

        node.process(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload)));
        assertNull(connection.captured());


        sensorPayload = new SensorPayload(1L, "device-1", 2L, 3L,
                "door", 4.0, null, "2026-08-03T10:00:00");

        node.process(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload)));
        assertNull(connection.captured());

        node.process(new Message(Map.of()));
        assertNull(connection.captured());
    }
}