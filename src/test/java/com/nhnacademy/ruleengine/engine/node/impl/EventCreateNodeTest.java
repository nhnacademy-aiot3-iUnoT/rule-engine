package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventDecisionDto;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventCreateNodeTest {

    private EventCreateNode node;

    @BeforeEach
    void setUp() {
        node = new EventCreateNode("event-create");
    }

    @Test
    @DisplayName("ruleResult와 eventDecision을 통해 환경 상태 이벤트를 생성한다")
    void createEvent() throws Exception {
        // given
        LocalConnection connection = connect(node);

        // when
        node.process(messageWith(ruleResult(), eventDecision()));

        // then
        EnvironmentStatusEventDto event =
                connection.poll().get(MessageFields.ENVIRONMENT_STATUS_EVENT);

        assertNotNull(event);

        assertAll(
                () -> assertEquals(1L, event.organizationId()),
                () -> assertEquals("device-eui", event.deviceEui()),
                () -> assertEquals(2L, event.storageId()),
                () -> assertEquals("temperature", event.sensorType()),
                () -> assertEquals(ViolationType.ABOVE_MAX, event.violationType()),
                () -> assertEquals(21.5, event.value()),
                () -> assertEquals(0.0, event.min()),
                () -> assertEquals(20.0, event.max()),
                () -> assertEquals("celsius", event.unit()),
                () -> assertEquals("2026-08-12T00:00:00Z", event.measuredAt()),
                () -> assertEquals("기준 온도를 초과했습니다.", event.message())
        );
    }


    @Test
    @DisplayName("ruleResult가 없으면 예외 없이 리턴")
    void stopWhenRuleResultIsMissing() {
        // given
        LocalConnection connection = connect(node);

        Message message = new Message(
                Map.of(MessageFields.ENVIRONMENT_EVENT_DECISION, eventDecision())
        );

        // when
        assertDoesNotThrow(() -> node.process(message));

        // then
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("eventDecision이 없으면 예외 없이 리턴")
    void stopWhenEventDecisionIsMissing() {
        // given
        LocalConnection connection = connect(node);

        Message message = new Message(
                Map.of(MessageFields.RULE_RESULT, ruleResult())
        );

        // when
        assertDoesNotThrow(() -> node.process(message));

        // then
        assertEquals(0, connection.getBufferSize());
    }

    private LocalConnection connect(AbstractNode node) {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        return connection;
    }

    private Message messageWith(
            RuleResultDto ruleResult,
            EnvironmentEventDecisionDto eventDecision
    ) {
        return new Message(Map.of(
                MessageFields.RULE_RESULT, ruleResult,
                MessageFields.ENVIRONMENT_EVENT_DECISION, eventDecision
        ));
    }

    private RuleResultDto ruleResult() {
        return new RuleResultDto(
                1L,
                "device-eui",
                2L,
                3L,
                "temperature",
                ViolationType.ABOVE_MAX,
                true,
                21.5,
                0.0,
                20.0,
                "celsius",
                "2026-08-12T00:00:00Z",
                5,
                "기준 온도를 초과했습니다."
        );
    }

    private EnvironmentEventDecisionDto eventDecision() {
        return new EnvironmentEventDecisionDto(
                EnvStatus.NORMAL,
                EnvStatus.WARNING,
                EnvironmentEventReason.STATUS_CHANGED
        );
    }
}
