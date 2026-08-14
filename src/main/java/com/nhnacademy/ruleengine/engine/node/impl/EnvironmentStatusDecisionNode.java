package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentDecisionState;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventDecisionDto;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorKeys;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EnvironmentStatusDecisionNode extends AbstractNode {

    // decide()의 결과: 다음 상태와, 발행할 이벤트(없으면 null)
    private record Transition(
            EnvironmentDecisionState nextState,
            EnvironmentEventDecisionDto event
    ) {
    }

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final int ALERT_INTERVAL = 30;  //lastAlertAt 타임 갱신주기

    // 외부 장치/게이트웨이의 시계가 앞서있는 경우, 미래 타임스탬프 하나가 상태를 영구히 막아버리는 것을 방지한다.
    private static final Duration FUTURE_TOLERANCE = Duration.ofMinutes(1);

    private final EnvironmentDecisionStateRedisRepository stateRepository;

    public EnvironmentStatusDecisionNode(
            String id,
            EnvironmentDecisionStateRedisRepository stateRepository
    ) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        this.stateRepository = stateRepository;
    }

    @Override
    protected void onProcess(Message message) {
        RuleResultDto ruleResult = message.get(MessageFields.RULE_RESULT);

        // 상위 노드는 ruleResult가 있을 때만 전달하므로 여기 걸리면 배선이나 payload 계약이 깨진 것이다.
        if (ruleResult == null) {
            log.error("[{}] ruleResult가 없습니다. 상위 노드의 payload 계약이 깨졌습니다.", getId());
            return;
        }

        String zoneKey = SensorKeys.zoneOf(
                ruleResult.organizationId(),
                ruleResult.storageId(),
                ruleResult.zoneId()
        );

        String sensorField = SensorKeys.sensorFieldOf(
                ruleResult.deviceEui(),
                ruleResult.sensorType()
        );

        processRuleDecision(ruleResult, zoneKey, sensorField).ifPresent(
                environmentEventDecisionDto -> send(
                        OUTPUT_PORT,
                        message.withPayload(
                                Map.of(
                                        MessageFields.RULE_RESULT, ruleResult,
                                        MessageFields.ENVIRONMENT_EVENT_DECISION, environmentEventDecisionDto))
                )
        );
    }

    private Optional<EnvironmentEventDecisionDto> processRuleDecision(
            RuleResultDto ruleResult,
            String zoneKey,
            String sensorField
    ) {
        LocalDateTime measuredAt = parseMeasuredAt(ruleResult.measuredAt()); // 측정한 시간

        EnvironmentDecisionState oldState = stateRepository.find(zoneKey, sensorField).orElse(null); // 이전상태 불러오기
        Transition transition = decide(oldState, ruleResult, measuredAt);

        if (transition.nextState() != null) {
            stateRepository.save(zoneKey, sensorField, transition.nextState());
        }

        return Optional.ofNullable(transition.event());
    }

    // 형식이 깨진 경우 메시지 처리 자체가 죽지 않도록 현재 시간으로 대체한다.
    private LocalDateTime parseMeasuredAt(String measuredAt) {
        try {
            return LocalDateTime.ofInstant(Instant.parse(measuredAt), ZoneOffset.UTC);
        } catch (DateTimeParseException | NullPointerException exception) {
            log.warn("[{}] measuredAt 형식이 올바르지 않아 현재 시간으로 처리합니다. measuredAt={}", getId(), measuredAt);
            return LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    private Transition decide(EnvironmentDecisionState oldState, RuleResultDto ruleResult, LocalDateTime measuredAt) {
        // 들아온 데이터가 기존 데이터보다 과거인 경우 데이터 버림
        if (oldState != null
                && oldState.lastMeasuredAt() != null
                && measuredAt.isBefore(oldState.lastMeasuredAt())) {
            log.info("[{}] 과거 측정 메시지라 상태 전이를 건너뜁니다. lastMeasuredAt={}, measuredAt={}, zoneId={}, sensorType={}",
                    getId(),
                    oldState.lastMeasuredAt(),
                    measuredAt,
                    ruleResult.zoneId(),
                    ruleResult.sensorType()
            );
            return new Transition(oldState, null);
        }

        // 외부 장치/게이트웨이 시계가 앞서있는 미래 타임스탬프는 신뢰하지 않고 버린다.
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (measuredAt.isAfter(now.plus(FUTURE_TOLERANCE))) {
            log.warn("[{}] 미래 측정 메시지라 상태 전이를 건너뜁니다. now={}, measuredAt={}, zoneId={}, sensorType={}",
                    getId(),
                    now,
                    measuredAt,
                    ruleResult.zoneId(),
                    ruleResult.sensorType()
            );
            return new Transition(oldState, null);
        }

        EnvStatus previousStatus = oldState == null ? EnvStatus.NORMAL : oldState.state(); // 이전 상태불러오기

        // 정상 결과인경우
        if (!ruleResult.violated()) {
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvStatus.NORMAL, null, null, measuredAt);
            if (previousStatus == EnvStatus.NORMAL) {
                return new Transition(next, null); // Normal 상태 유지인경우 event 생성안함
            }
            return new Transition(next, createEventDecision(previousStatus, EnvStatus.NORMAL, EnvironmentEventReason.STATUS_CHANGED));
        }

        Integer durationMinutes = ruleResult.durationMinutes(); // 임계시간 불러오기

        // 임계시간 조건이 없는경우 NORMAL -> CRITICAL 로 변경
        if (durationMinutes == null || durationMinutes <= 0) {
            return decideInstantViolation(oldState, previousStatus, measuredAt);
        }

        // 임계시간 조건이 있는경우 NORMAL -> WARNING -> CRITICAL로 변경
        return decideDurationBasedViolation(oldState, previousStatus, durationMinutes, measuredAt);
    }

    // 위반 지속시간 조건이 없는 경우 즉시 CRITICAL
    private Transition decideInstantViolation(EnvironmentDecisionState oldState, EnvStatus previousStatus, LocalDateTime measuredAt) {
        // 최초 CRITICAL 상태 변화시 다음노드로 전송
        if (oldState == null || oldState.state() != EnvStatus.CRITICAL) {
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvStatus.CRITICAL, null, measuredAt, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvStatus.CRITICAL, EnvironmentEventReason.STATUS_CHANGED));
        }

        // 이미 CRITICAL이면 ALERT_INTERVAL마다 반복 알림
        if (Duration.between(oldState.lastAlertAt(), measuredAt).toMinutes() >= ALERT_INTERVAL) {
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvStatus.CRITICAL, null, measuredAt, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvStatus.CRITICAL, EnvironmentEventReason.CRITICAL_REPEATED));
        }
        return new Transition(updateLastMeasuredAt(oldState, measuredAt), null);
    }

    // 위반 지속시간 조건이 있는 경우 NORMAL -> WARNING -> CRITICAL 순으로 전이
    private Transition decideDurationBasedViolation(
            EnvironmentDecisionState oldState,
            EnvStatus previousStatus,
            int durationMinutes,
            LocalDateTime measuredAt
    ) {
        // 첫 위반 발생 또는 상태가 NORMAL일 때 WARNING으로 진입
        if (oldState == null || oldState.state() == EnvStatus.NORMAL) {
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvStatus.WARNING, measuredAt, null, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvStatus.WARNING, EnvironmentEventReason.STATUS_CHANGED));
        }

        // 위반 지속시간 초과시 CRITICAL 전환
        if (oldState.state() == EnvStatus.WARNING) {
            long elapsedMinutes = Duration.between(oldState.firstViolatedAt(), measuredAt).toMinutes();
            if (elapsedMinutes >= durationMinutes) {
                EnvironmentDecisionState next = new EnvironmentDecisionState(EnvStatus.CRITICAL, oldState.firstViolatedAt(), measuredAt, measuredAt);
                return new Transition(next, createEventDecision(previousStatus, EnvStatus.CRITICAL, EnvironmentEventReason.STATUS_CHANGED));
            }
            return new Transition(updateLastMeasuredAt(oldState, measuredAt), null);
        }

        // 기존 상태가 CRITICAL인 경우, ALERT_INTERVAL마다 반복 알림
        long elapsedMinutes = Duration.between(oldState.lastAlertAt(), measuredAt).toMinutes();
        if (elapsedMinutes >= ALERT_INTERVAL) {
            EnvironmentDecisionState next = new EnvironmentDecisionState(oldState.state(), oldState.firstViolatedAt(), measuredAt, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvStatus.CRITICAL, EnvironmentEventReason.CRITICAL_REPEATED));
        }
        return new Transition(updateLastMeasuredAt(oldState, measuredAt), null);
    }

    private EnvironmentEventDecisionDto createEventDecision(
            EnvStatus previousStatus,
            EnvStatus currentStatus,
            EnvironmentEventReason reason
    ) {
        return new EnvironmentEventDecisionDto(previousStatus, currentStatus, reason);
    }

    private EnvironmentDecisionState updateLastMeasuredAt(
            EnvironmentDecisionState oldState,
            LocalDateTime measuredAt
    ) {
        return new EnvironmentDecisionState(oldState.state(), oldState.firstViolatedAt(), oldState.lastAlertAt(), measuredAt);
    }
}
