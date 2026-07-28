package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.RuleResultDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

// RuleResultDto를 받아 NORMAL / PENDING / ALERT 상태 판단
@Slf4j
public class AbnormalDecisionNode extends AbstractNode {
    private enum RuleStates{
        NORMAL,
        PENDING,
        ALERT
    }

    private record DecisionState(
            RuleStates state,
            LocalDateTime firstViolatedAt,
            LocalDateTime lastAlertAt
    ) {}

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final Integer ALERT_INTERVAL = 30;  //lastAlertAt 타임 갱신주기
    private final ConcurrentHashMap<String, DecisionState> decisionStates = new ConcurrentHashMap<>();

    public AbnormalDecisionNode(String id) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        RuleResultDto ruleResult  = message.get(MessageFields.RULE_RESULT);

        if (ruleResult == null) {
            log.info("[{}] ruleResult가 없어 상태판단을 건너뜁니다.", getId());
            return;
        }

        String sensorType = ruleResult.sensorType();
        String key = ruleResult.organizationId() + ':' +
                ruleResult.storageId() + ':' +
                ruleResult.sectionId() + ':' +
                ruleResult.deviceEui() + ':' +
                sensorType;

        boolean shouldSend = processRuleDecision(ruleResult, key);

        if(shouldSend){
            send(OUTPUT_PORT, new Message(Map.of(MessageFields.RULE_RESULT, ruleResult)));
        }
    }

    private boolean processRuleDecision(RuleResultDto ruleResult, String key){
        Integer durationMinutes = ruleResult.durationMinutes();
        LocalDateTime now = LocalDateTime.now();
        AtomicBoolean shouldSend = new AtomicBoolean(false);

        decisionStates.compute(key, (k, oldState) -> {
            // 정상 결과
            if(!ruleResult.violated()){
                return new DecisionState(RuleStates.NORMAL, null, null);
            }

            // 위반 지속시간 조건이 없는 경우(door open)는 바로 ALERT
            if(durationMinutes == null || durationMinutes<=0){
                if(oldState == null || oldState.state() == RuleStates.NORMAL){
                    shouldSend.set(true);
                    return new DecisionState(RuleStates.ALERT, null, now);
                }

                if (oldState.state() == RuleStates.ALERT) {
                    if (oldState.lastAlertAt() != null
                            && Duration.between(oldState.lastAlertAt(), now).toMinutes() >= ALERT_INTERVAL) {
                        shouldSend.set(true);
                        return new DecisionState(RuleStates.ALERT, null, now);
                    }
                    return oldState;
                }

                shouldSend.set(true);
                return new DecisionState(RuleStates.ALERT, null, now);
            }

            // 첫 위반 발생(door는 위 조건에서 걸려짐) or 상태가 NORMAL 일때 PENDING로 저장
            if(oldState == null || oldState.state() == RuleStates.NORMAL){
                return new DecisionState(RuleStates.PENDING, now, null);
            }

            // 위반 지속시간 초과시 ALERT 전환
            if(oldState.state() == RuleStates.PENDING){
                long elapsedMinutes = Duration.between(oldState.firstViolatedAt(), now).toMinutes();

                if(elapsedMinutes >= durationMinutes){
                    shouldSend.set(true);
                    return new DecisionState(RuleStates.ALERT, oldState.firstViolatedAt(), now);
                }

                return oldState;
            }

            // 기존 상태가 ALERT일 경우
            if(oldState.state() == RuleStates.ALERT){
                long elapsedMinutes = Duration.between(oldState.lastAlertAt(), now).toMinutes();

                // lastAlertAt 갱신 (ALERT_INTERVAL 마다)
                if(elapsedMinutes >= ALERT_INTERVAL){
                    shouldSend.set(true);
                    return new DecisionState(oldState.state(), oldState.firstViolatedAt(), now);
                }
                return oldState;
            }

            return oldState;
        });
        return shouldSend.get();
    }

}