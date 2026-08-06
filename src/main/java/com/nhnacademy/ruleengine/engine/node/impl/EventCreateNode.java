package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventDecisionDto;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

// 룰 판단 결과를 외부 이벤트 계약으로 확정하는 경계
// 환경 상태 이벤트 DTO를 생성한다.
@Slf4j
public class EventCreateNode extends AbstractNode {
    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    public EventCreateNode(String id) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        RuleResultDto ruleResult = message.get(MessageFields.RULE_RESULT);
        EnvironmentEventDecisionDto eventDecision = message.get(MessageFields.ENVIRONMENT_EVENT_DECISION);

        if(ruleResult==null){
            log.info("[{}] ruleResult가 없어 이벤트 생성을 건너뜁니다.", getId());
            return;
        }

        if(eventDecision==null){
            log.info("[{}] environmentEventDecision가 없어 이벤트 생성을 건너뜁니다.", getId());
            return;
        }

        EnvironmentStatusEventDto event = EnvironmentStatusEventDto.from(ruleResult, eventDecision);

        send(OUTPUT_PORT, message.withEntry(MessageFields.ENVIRONMENT_STATUS_EVENT, event));

        log.info(
                "[{}] 환경 상태 이벤트 생성. organizationId={}, storageId={}, sectionId={}, deviceEui={}, sensorType={}, previousStatus={}, currentStatus={}, reason={}",
                getId(),
                event.organizationId(),
                event.storageId(),
                event.sectionId(),
                event.deviceEui(),
                event.sensorType(),
                event.previousStatus(),
                event.currentStatus(),
                eventDecision.reason()
        );
    }
}
