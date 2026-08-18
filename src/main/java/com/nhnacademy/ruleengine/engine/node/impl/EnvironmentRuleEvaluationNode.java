package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.command.rule.EnvironmentRuleCommand;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;

// 센서 타입에 맞는 룰 Command를 찾아 룰 판단을 수행하는 노드
@Slf4j
public class EnvironmentRuleEvaluationNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    // 센서별 룰 판단을 담당하는 Command 목록
    private final List<EnvironmentRuleCommand> commands;

    public EnvironmentRuleEvaluationNode(String id, List<EnvironmentRuleCommand> commands) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
        this.commands = commands;
    }

    @Override
    protected void onProcess(Message message) {

        // Message에서 센서 데이터 가져오기
        SensorPayload sensorPayload = message.get(MessageFields.SENSOR_PAYLOAD);

        // 센서 데이터가 없으면 종료
        if (sensorPayload == null) {
            log.error("[{}] sensorPayload가 없습니다.", getId());
            return;
        }

        // 센서 타입 확인
        String sensorType = sensorPayload.sensorType();

        // 센서 타입을 처리할 Command 찾기
        Optional<EnvironmentRuleCommand> command = commands.stream()
                .filter(candidate -> candidate.supports(sensorType))
                .findFirst();

        // 처리할 Command가 없으면 종료
        if (command.isEmpty()) {
            log.warn("[{}] 처리할 수 없는 sensorType={}", getId(), sensorType);
            return;
        }

        // Command에게 룰 판단 위임
        Optional<RuleResultDto> ruleResult = command.get().evaluate(sensorPayload);

        // 판단 결과가 없으면 종료
        if (ruleResult.isEmpty()) {
            return;
        }

        // 룰 결과를 Message에 담아 다음 노드로 전달
        send(
                OUTPUT_PORT,
                message.withPayload(
                        Map.of(MessageFields.RULE_RESULT, ruleResult.get())
                )
        );
    }
}