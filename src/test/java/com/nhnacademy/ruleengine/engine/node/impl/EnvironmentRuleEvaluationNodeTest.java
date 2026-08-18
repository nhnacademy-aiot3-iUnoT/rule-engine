package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.command.rule.EnvironmentRuleCommand;
import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.FlowProcessingCompletion;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EnvironmentRuleEvaluationNodeTest {

    @Test
    @DisplayName("sensorType을 지원하는 Command에 룰 판단을 위임한다")
    void delegateToSupportingCommand() {
        // given
        SensorPayload sensorPayload = sensorPayload("temperature");
        EnvironmentRuleCommand command = command(true, Optional.of(ruleResult()));

        EnvironmentRuleEvaluationNode node =
                new EnvironmentRuleEvaluationNode("rule", List.of(command));

        // when
        node.process(messageWith(sensorPayload));

        // then
        verify(command).supports("temperature");
        verify(command).evaluate(sensorPayload);
    }

    @Test
    @DisplayName("판단 결과를 ruleResult로 담아 out 포트로 전달한다")
    void sendRuleResult() throws Exception {
        // given
        RuleResultDto ruleResult = ruleResult();
        EnvironmentRuleEvaluationNode node = new EnvironmentRuleEvaluationNode(
                "rule",
                List.of(command(true, Optional.of(ruleResult)))
        );

        LocalConnection connection = connect(node);

        // when
        node.process(messageWith(sensorPayload("temperature")));

        // then
        Message sent = connection.poll();

        assertSame(ruleResult, sent.get(MessageFields.RULE_RESULT));
    }

    @Test
    @DisplayName("전달하는 메시지에는 sensorPayload가 남지 않는다")
    void payloadIsReplaced() throws Exception {
        // given
        EnvironmentRuleEvaluationNode node = new EnvironmentRuleEvaluationNode(
                "rule",
                List.of(command(true, Optional.of(ruleResult())))
        );

        LocalConnection connection = connect(node);

        Message message = messageWith(sensorPayload("temperature"));

        // when
        node.process(message);

        // then
        Message sent = connection.poll();

        assertFalse(sent.hasEntry(MessageFields.SENSOR_PAYLOAD));
        assertEquals(message.getUuid(), sent.getUuid());
    }

    @Test
    @DisplayName("sensorPayload가 없으면 예외 없이 흐름을 끝낸다")
    void stopWhenSensorPayloadIsMissing() {
        // given
        EnvironmentRuleCommand command = command(true, Optional.of(ruleResult()));

        EnvironmentRuleEvaluationNode node =
                new EnvironmentRuleEvaluationNode("rule", List.of(command));

        LocalConnection connection = connect(node);

        // when
        assertDoesNotThrow(() -> node.process(new Message(new HashMap<>())));

        // then
        verify(command, never()).evaluate(any());
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("지원하는 Command가 없으면 다음 노드로 보내지 않는다")
    void stopWhenNoCommandSupportsSensorType() {
        // given
        EnvironmentRuleCommand command = command(false, Optional.of(ruleResult()));

        EnvironmentRuleEvaluationNode node =
                new EnvironmentRuleEvaluationNode("rule", List.of(command));

        LocalConnection connection = connect(node);

        // when
        node.process(messageWith(sensorPayload("co2")));

        // then
        verify(command, never()).evaluate(any());
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("판단할 룰이 없으면 다음 노드로 보내지 않는다")
    void stopWhenRuleResultIsEmpty() {
        // given
        EnvironmentRuleEvaluationNode node = new EnvironmentRuleEvaluationNode(
                "rule",
                List.of(command(true, Optional.empty()))
        );

        LocalConnection connection = connect(node);

        // when
        node.process(messageWith(sensorPayload("temperature")));

        // then
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("다음 노드로 보내지 않으면 이 노드에서 처리를 완료한다")
    void completeProcessingWhenNotForwarded() {
        // given
        EnvironmentRuleEvaluationNode node = new EnvironmentRuleEvaluationNode(
                "rule",
                List.of(command(true, Optional.empty()))
        );

        FlowProcessingCompletion completion = new FlowProcessingCompletion();

        Message message = new Message(
                Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload("temperature")),
                completion
        );

        // when
        node.process(message);

        // then
        assertDoesNotThrow(() -> completion.await(Duration.ofMillis(100)));
    }

    private LocalConnection connect(AbstractNode node) {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        return connection;
    }

    private EnvironmentRuleCommand command(
            boolean supports,
            Optional<RuleResultDto> ruleResult
    ) {
        EnvironmentRuleCommand command = mock(EnvironmentRuleCommand.class);

        when(command.supports(any())).thenReturn(supports);

        if (supports) {
            when(command.evaluate(any())).thenReturn(ruleResult);
        }

        return command;
    }

    private Message messageWith(SensorPayload sensorPayload) {
        return new Message(Map.of(MessageFields.SENSOR_PAYLOAD, sensorPayload));
    }

    private SensorPayload sensorPayload(String sensorType) {
        return new SensorPayload(
                1L,
                "device-eui",
                1L,
                1L,
                sensorType,
                21.5,
                "celsius",
                "2026-08-12T00:00:00Z"
        );
    }

    private RuleResultDto ruleResult() {
        return new RuleResultDto(
                1L,
                "device-eui",
                1L,
                1L,
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
}
