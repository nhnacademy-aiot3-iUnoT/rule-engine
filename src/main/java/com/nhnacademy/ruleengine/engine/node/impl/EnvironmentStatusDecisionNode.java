package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.*;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import com.nhnacademy.ruleengine.engine.service.RedisLeaseLockService;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// RuleResultDto를 받아 NORMAL / WARNING / CRITICAL 상태 판단하여 환경상태 전이, EnvironmentEventDecisionDto 생성
// 서버가 이중화되어 같은 센서의 메시지가 서로 다른 인스턴스로 분배될 수 있으므로,
// 판단 상태(EnvironmentDecisionState)는 로컬 메모리가 아닌 Redis에 저장해 인스턴스 간 공유하고,
// 센서 키 단위 Redis Lock으로 읽기-판단-쓰기 구간의 원자성을 보장한다.
@Slf4j
public class EnvironmentStatusDecisionNode extends AbstractNode {

    // decide()의 결과: 다음 상태와, 발행할 이벤트(없으면 null)
    private record Transition(
            EnvironmentDecisionState nextState,
            EnvironmentEventDecisionDto event
    ) {}

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final int ALERT_INTERVAL = 30;  //lastAlertAt 타임 갱신주기

    private static final String LOCK_KEY_PREFIX = "rule-engine:env-status:lock:";
    private static final Duration LOCK_LEASE = Duration.ofSeconds(3);
    private static final int LOCK_MAX_ATTEMPTS = 10;
    private static final Duration LOCK_RETRY_DELAY = Duration.ofMillis(50);

    private final EnvironmentDecisionStateRedisRepository stateRepository;
    private final RedisLeaseLockService lockService;

    public EnvironmentStatusDecisionNode(
            String id,
            EnvironmentDecisionStateRedisRepository stateRepository,
            RedisLeaseLockService lockService
    ) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        this.stateRepository = stateRepository;
        this.lockService = lockService;
    }

    @Override
    protected void onProcess(Message message) {
        RuleResultDto ruleResult  = message.get(MessageFields.RULE_RESULT);

        if (ruleResult == null) {
            log.info("[{}] ruleResult가 없어 상태판단을 건너뜁니다.", getId());
            return;
        }

        String key = ruleResult.organizationId() + ':' +
                ruleResult.storageId() + ':' +
                ruleResult.zoneId() + ':' +
                ruleResult.deviceEui() + ':' +
                ruleResult.sensorType();

        Optional<EnvironmentEventDecisionDto> statusChange = processRuleDecision(ruleResult, key);

        // 상태 변화 발생시 다음 Node로 전송
        statusChange.ifPresent(environmentEventDecisionDto -> send(
                OUTPUT_PORT,
                message.withPayload(
                        Map.of(
                                MessageFields.RULE_RESULT, ruleResult,
                                MessageFields.ENVIRONMENT_EVENT_DECISION, environmentEventDecisionDto))
        ));
    }

    private Optional<EnvironmentEventDecisionDto> processRuleDecision(RuleResultDto ruleResult, String key){
        LocalDateTime measuredAt = LocalDateTime.parse(ruleResult.measuredAt()); // 측정한 시간

        String lockKey = LOCK_KEY_PREFIX + key;
        String ownerToken = UUID.randomUUID().toString();

        // 상태를 변경하기위해 Lock을 시도
        if (!acquireLock(lockKey, ownerToken)) {
            log.warn("[{}] key={} 상태에 대한 Redis Lock 획득에 실패해 상태판단을 건너뜁니다.", getId(), key);
            return Optional.empty();
        }

        try {
            EnvironmentDecisionState oldState = stateRepository.find(key).orElse(null); // 이전상태 불러오기
            Transition transition = decide(oldState, ruleResult, measuredAt);
            stateRepository.save(key, transition.nextState());
            return Optional.ofNullable(transition.event());
        } finally {
            lockService.release(lockKey, ownerToken); // 락해제
        }
    }

    // Lock이 다른 인스턴스에 점유되어 있을 경우 짧게 재시도한다.
    private boolean acquireLock(String lockKey, String ownerToken) {
        for (int attempt = 0; attempt < LOCK_MAX_ATTEMPTS; attempt++) {
            if (lockService.acquire(lockKey, ownerToken, LOCK_LEASE)) {
                return true;
            }

            try {
                Thread.sleep(LOCK_RETRY_DELAY.toMillis()); // 락 재시도
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private Transition decide(EnvironmentDecisionState oldState, RuleResultDto ruleResult, LocalDateTime measuredAt){
        // 들아온 데이터가 기존 데이터보다 과거인 경우 데이터 버림
        if (oldState != null
                && oldState.lastMeasuredAt() != null
                && measuredAt.isBefore(oldState.lastMeasuredAt())){
            log.info("[{}] 과거 측정 메시지라 상태 전이를 건너뜁니다. lastMeasuredAt={}, measuredAt={}",
                    getId(),
                    oldState.lastMeasuredAt(),
                    measuredAt
            );
            return new Transition(oldState, null);
        }

        EnvironmentStatus previousStatus = oldState == null ? EnvironmentStatus.NORMAL : oldState.state(); // 이전 상태불러오기

        // 정상 결과인경우
        if(!ruleResult.violated()){
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvironmentStatus.NORMAL, null, null, measuredAt);
            if (previousStatus == EnvironmentStatus.NORMAL) {
                return new Transition(next, null); // Normal 상태 유지인경우 event 생성안함
            }
            return new Transition(next, createEventDecision(previousStatus, EnvironmentStatus.NORMAL, EnvironmentEventReason.STATUS_CHANGED));
        }

        Integer durationMinutes = ruleResult.durationMinutes(); // 임계시간 불러오기

        // 임계시간 조건이 없는경우 NORMAL -> CRITICAL 로 변경
        if(durationMinutes == null || durationMinutes <= 0){
            return decideInstantViolation(oldState, previousStatus, measuredAt);
        }

        // 임계시간 조건이 있는경우 NORMAL -> WARNING -> CRITICAL로 변경
        return decideDurationBasedViolation(oldState, previousStatus, durationMinutes, measuredAt);
    }

    // 위반 지속시간 조건이 없는 경우 즉시 CRITICAL
    private Transition decideInstantViolation(EnvironmentDecisionState oldState, EnvironmentStatus previousStatus, LocalDateTime measuredAt){
        // 최초 CRITICAL 상태 변화시 다음노드로 전송
        if(oldState == null || oldState.state() != EnvironmentStatus.CRITICAL){
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvironmentStatus.CRITICAL, null, measuredAt, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvironmentStatus.CRITICAL, EnvironmentEventReason.STATUS_CHANGED));
        }

        // 이미 CRITICAL이면 ALERT_INTERVAL마다 반복 알림
        if(Duration.between(oldState.lastAlertAt(), measuredAt).toMinutes() >= ALERT_INTERVAL){
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvironmentStatus.CRITICAL, null, measuredAt, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvironmentStatus.CRITICAL, EnvironmentEventReason.CRITICAL_REPEATED));
        }
        return new Transition(updateLastMeasuredAt(oldState, measuredAt), null);
    }

    // 위반 지속시간 조건이 있는 경우 NORMAL -> WARNING -> CRITICAL 순으로 전이
    private Transition decideDurationBasedViolation(
            EnvironmentDecisionState oldState,
            EnvironmentStatus previousStatus,
            int durationMinutes,
            LocalDateTime measuredAt
    ){
        // 첫 위반 발생 또는 상태가 NORMAL일 때 WARNING으로 진입
        if(oldState == null || oldState.state() == EnvironmentStatus.NORMAL){
            EnvironmentDecisionState next = new EnvironmentDecisionState(EnvironmentStatus.WARNING, measuredAt, null, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvironmentStatus.WARNING, EnvironmentEventReason.STATUS_CHANGED));
        }

        // 위반 지속시간 초과시 CRITICAL 전환
        if(oldState.state() == EnvironmentStatus.WARNING){
            long elapsedMinutes = Duration.between(oldState.firstViolatedAt(), measuredAt).toMinutes();
            if(elapsedMinutes >= durationMinutes){
                EnvironmentDecisionState next = new EnvironmentDecisionState(EnvironmentStatus.CRITICAL, oldState.firstViolatedAt(), measuredAt, measuredAt);
                return new Transition(next, createEventDecision(previousStatus, EnvironmentStatus.CRITICAL, EnvironmentEventReason.STATUS_CHANGED));
            }
            return new Transition(updateLastMeasuredAt(oldState, measuredAt), null);
        }

        // 기존 상태가 CRITICAL인 경우, ALERT_INTERVAL마다 반복 알림
        long elapsedMinutes = Duration.between(oldState.lastAlertAt(), measuredAt).toMinutes();
        if(elapsedMinutes >= ALERT_INTERVAL){
            EnvironmentDecisionState next = new EnvironmentDecisionState(oldState.state(), oldState.firstViolatedAt(), measuredAt, measuredAt);
            return new Transition(next, createEventDecision(previousStatus, EnvironmentStatus.CRITICAL, EnvironmentEventReason.CRITICAL_REPEATED));
        }
        return new Transition(updateLastMeasuredAt(oldState, measuredAt), null);
    }

    private EnvironmentEventDecisionDto createEventDecision(
            EnvironmentStatus previousStatus,
            EnvironmentStatus currentStatus,
            EnvironmentEventReason reason
    ){
        return new EnvironmentEventDecisionDto(previousStatus, currentStatus, reason);
    }

    private EnvironmentDecisionState updateLastMeasuredAt(
            EnvironmentDecisionState oldState,
            LocalDateTime measuredAt
    ){
        return new EnvironmentDecisionState(oldState.state(), oldState.firstViolatedAt(), oldState.lastAlertAt(), measuredAt);
    }
}
