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

// 센서타입에 맞는 EnvironmentRuleCommand를 찾아 룰 판단을 위임한다.
// 담당 Command가 없거나 판단할 룰이 없으면 다음 노드로 보내지 않고 끝낸다.
@Slf4j
public class EnvironmentRuleEvaluationNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final List<EnvironmentRuleCommand> commands;

    public EnvironmentRuleEvaluationNode(String id, List<EnvironmentRuleCommand> commands) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
        this.commands = commands;
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayload sensorPayload = message.get(MessageFields.SENSOR_PAYLOAD);

        if (sensorPayload == null) {
            log.info("[{}] sensorPayload가 없어 룰 판단을 건너뜁니다.", getId());
            return;
        }

        String sensorType = sensorPayload.sensorType();
        Optional<EnvironmentRuleCommand> command = commands.stream()
                .filter(candidate -> candidate.supports(sensorType))
                .findFirst();

        if (command.isEmpty()) {
            log.warn("[{}] 처리 대상이 아닌 sensorType이라 룰 판단을 건너뜁니다. sensorType={}, deviceEui={}",
                    getId(),
                    sensorType,
                    sensorPayload.deviceEui()
            );
            return;
        }

        Optional<RuleResultDto> ruleResult = command.get().evaluate(sensorPayload);

        if (ruleResult.isEmpty()) {
            return;
        }

        send(OUTPUT_PORT, message.withPayload(Map.of(MessageFields.RULE_RESULT, ruleResult.get())));
    }
}
