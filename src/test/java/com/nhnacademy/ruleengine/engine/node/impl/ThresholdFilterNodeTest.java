package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import com.nhnacademy.ruleengine.testsupport.CapturingConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ThresholdFilterNodeTest {
    private ThresholdFilterNode node;
    private CapturingConnection connection;
    private SensorPayload payload;
    private RuleResultDto result;

    @Mock
    ThresholdPolicyService thresholdPolicyService;

    @BeforeEach
    void setUp(){
        node = new ThresholdFilterNode("threshold", thresholdPolicyService);
        connection = new CapturingConnection();
        node.getOutputPort("out").connect(connection);
    }

    @Test
    void temperature가_min보다_작으면_BELOW_MIN_결과를_보낸다(){
        givenThresholdPolicy();
        payload = sensorPayload("temperature", 2.0);
        node.onProcess(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, payload)));
        RuleResultDto result = connection.captured().get(MessageFields.RULE_RESULT);

        assertEquals("temperature", result.sensorType());
        assertEquals(ViolationType.BELOW_MIN, result.violationType());
        assertTrue(result.violated());
        assertEquals(5.0, result.min());
        assertEquals(10.0, result.max());
        assertEquals(5, result.durationMinutes());
    }

    @Test
    void temperature가_max보다_크면_ABOVE_MAX_결과를_보낸다(){
        givenThresholdPolicy();
        payload = sensorPayload("temperature", 20.0);
        node.onProcess(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, payload)));
        result = connection.captured().get(MessageFields.RULE_RESULT);

        assertEquals("temperature", result.sensorType());
        assertEquals(ViolationType.ABOVE_MAX, result.violationType());
        assertTrue(result.violated());
        assertEquals(5.0, result.min());
        assertEquals(10.0, result.max());
        assertEquals(5, result.durationMinutes());
    }

    @Test
    void temperature가_정상범위일경우(){
        givenThresholdPolicy();
        payload = sensorPayload("temperature", 7.0);
        node.onProcess(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, payload)));
        result = connection.captured().get(MessageFields.RULE_RESULT);

        assertEquals("temperature", result.sensorType());
        assertEquals(ViolationType.NORMAL, result.violationType());
        assertFalse(result.violated());
        assertEquals(5.0, result.min());
        assertEquals(10.0, result.max());
        assertEquals(5, result.durationMinutes());
    }

    @Test
    void 메세지를_보내지_않는경우(){
        // door 같은 비대상 sensorType이면 메시지를 보내지 않음
        payload = sensorPayload("door", 7.0);
        node.onProcess(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, payload)));
        assertNull(connection.captured());

        // thresholdPolicy가 null이면 메시지를 보내지 않음
        payload = sensorPayload("temperature", 7.0);
        node.onProcess(new Message(Map.of(MessageFields.SENSOR_PAYLOAD, payload)));
        assertNull(connection.captured());
    }

    private SensorPayload sensorPayload(String sensorType, double value) {
        return new SensorPayload(
                1L,
                "device-1",
                2L,
                3L,
                sensorType,
                value,
                "C",
                "2026-08-03T10:00:00"
        );
    }

    private void givenThresholdPolicy() {
        given(thresholdPolicyService.getThresholdPolicy(1L, 2L, 3L))
                .willReturn(new ThresholdPolicyDto(
                        1L, 2L, 3L,
                        5.0, 10.0,
                        30.0, 70.0,
                        5
                ));
    }

}