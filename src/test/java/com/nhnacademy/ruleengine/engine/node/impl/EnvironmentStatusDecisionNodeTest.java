package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentDecisionState;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventDecisionDto;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnvironmentStatusDecisionNodeTest {


    @Mock
    private EnvironmentDecisionStateRedisRepository repository;

    private EnvironmentStatusDecisionNode node;

    private Connection connection;

    private static final int DURATION_MINUTES = 30;

    private static final Instant BASE = Instant.parse("2024-01-01T00:00:00Z");

    @BeforeEach
    void setUp() {
        node = new EnvironmentStatusDecisionNode("test", repository);

        connection = mock(Connection.class);
        node.getOutputPort("out").connect(connection);

    }

    @Test
    @DisplayName("ruleResult 없는경우 저장,전송 둘다 하지않는다")
    void noRuleResult() {
        node.process(new Message(Map.of()));

        verify(repository, never()).save(anyString(), anyString(), any(EnvironmentDecisionState.class));
        verify(connection, never()).deliver(any(Message.class));


    }


    @Test
    @DisplayName("시간 형식이 깨질경우 현재시간으로 처리한다")
    void invalidTimestamp() {
        when(repository.find(anyString(), anyString())).thenReturn(Optional.empty());

        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, "no timestamp")));

        EnvironmentDecisionState capturedState = capturedState();

        assertAll(
                () -> assertEquals(EnvStatus.WARNING, capturedState.state()),
                () -> assertNotNull(capturedState.lastMeasuredAt()),
                () -> assertInstanceOf(Instant.class, capturedState.lastMeasuredAt()),
                () -> assertEquals(EnvStatus.WARNING, capturedEvent().currentStatus())

        );
    }

    @Test
    @DisplayName("이전 측정시간보다 과거인 경우 무시한다")
    void staleMessage() {
        EnvironmentDecisionState oldState =
                state(EnvStatus.WARNING, BASE, null, BASE.plus(Duration.ofMinutes(5)));

        when(repository.find(anyString(), anyString())).thenReturn(Optional.of(oldState));

        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, at(1))));

        assertEquals(capturedState(), oldState);

        verify(connection, never()).deliver(any(Message.class));
    }

    @Test
    @DisplayName("현재 시간보다 미래인 경우 무시한다")
    void futureMessage() {
        when(repository.find(anyString(), anyString())).thenReturn(Optional.empty());

        String future = Instant.now().plus(Duration.ofMinutes(5)).toString();
        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, future)));

        verify(repository, never()).save(anyString(), anyString(), any(EnvironmentDecisionState.class));
        verify(connection, never()).deliver(any(Message.class));

    }

    @Test
    @DisplayName("임계값을 넘지않은 경우엔 이벤트가 생성되지않는다")
    void normalMessage() {
        when(repository.find(anyString(), anyString())).thenReturn(Optional.of(state(EnvStatus.NORMAL, null, null, BASE)));

        node.process(messageOf(createRuleResult(false, DURATION_MINUTES, at(1))));

        assertEquals(EnvStatus.NORMAL, capturedState().state());
        verify(connection, never()).deliver(any(Message.class));

    }

    @Test
    @DisplayName("CRITICAL 상태에서 정상값이 들어와도 임계시간 전이면 상태가 유지된다")
    void criticalStaysWhileRecoveryPending() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.CRITICAL, BASE, BASE, BASE)));

        node.process(messageOf(createRuleResult(false, DURATION_MINUTES, at(1))));

        EnvironmentDecisionState saved = capturedState();

        assertAll(
                () -> assertEquals(EnvStatus.CRITICAL, saved.state()),
                () -> assertEquals(BASE, saved.firstViolatedAt()),
                () -> assertEquals(BASE, saved.lastAlertAt()),
                // 회복 대기 시작 시각을 기록한다.
                () -> assertEquals(BASE.plus(Duration.ofMinutes(1)), saved.firstNormalAt()),
                () -> assertEquals(BASE.plus(Duration.ofMinutes(1)), saved.lastMeasuredAt())
        );

        verify(connection, never()).deliver(any(Message.class));
    }

    @Test
    @DisplayName("CRITICAL 상태에서 임계시간 동안 정상이 유지되면 NORMAL로 해제된다")
    void criticalRecoversAfterDuration() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.CRITICAL, BASE, BASE, BASE.plus(Duration.ofMinutes(1)), BASE.plus(Duration.ofMinutes(5)))));

        node.process(messageOf(createRuleResult(false, DURATION_MINUTES, at(31))));

        EnvironmentDecisionState saved = capturedState();
        EnvironmentEventDecisionDto event = capturedEvent();

        assertAll(
                () -> assertEquals(EnvStatus.NORMAL, saved.state()),
                () -> assertNull(saved.firstViolatedAt()),
                () -> assertNull(saved.firstNormalAt()),
                () -> assertEquals(BASE.plus(Duration.ofMinutes(31)), saved.lastMeasuredAt()),
                () -> assertEquals(EnvStatus.CRITICAL, event.previousStatus()),
                () -> assertEquals(EnvStatus.NORMAL, event.currentStatus()),
                () -> assertEquals(EnvironmentEventReason.STATUS_CHANGED, event.reason())
        );
    }

    @Test
    @DisplayName("회복 대기중에 다시 위반이 들어오면 회복 시작 시각이 초기화된다")
    void recoveryResetOnViolation() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.CRITICAL, BASE, BASE.plus(Duration.ofMinutes(10)), BASE.plus(Duration.ofMinutes(10)), BASE.plus(Duration.ofMinutes(10)))));

        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, at(10))));

        EnvironmentDecisionState saved = capturedState();

        assertAll(
                () -> assertEquals(EnvStatus.CRITICAL, saved.state()),
                () -> assertNull(saved.firstNormalAt())
        );

        verify(connection, never()).deliver(any(Message.class));
    }

    @Test
    @DisplayName("임계시간 조건이 없으면 정상값 하나로 바로 NORMAL로 해제된다")
    void criticalRecoversImmediatelyWithoutDuration() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.CRITICAL, BASE, BASE, BASE)));

        node.process(messageOf(createRuleResult(false, null, at(1))));

        EnvironmentDecisionState saved = capturedState();
        EnvironmentEventDecisionDto event = capturedEvent();

        assertAll(
                () -> assertEquals(EnvStatus.NORMAL, saved.state()),
                () -> assertEquals(EnvStatus.CRITICAL, event.previousStatus()),
                () -> assertEquals(EnvStatus.NORMAL, event.currentStatus()),
                () -> assertEquals(EnvironmentEventReason.STATUS_CHANGED, event.reason())
        );
    }

    @Test
    @DisplayName("WARNING 상태에서 정상값이 들어오면 바로 NORMAL로 해제된다")
    void warningRecoversImmediately() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.WARNING, BASE, null, BASE)));

        node.process(messageOf(createRuleResult(false, DURATION_MINUTES, at(1))));

        assertAll(
                () -> assertEquals(EnvStatus.NORMAL, capturedState().state()),
                () -> assertEquals(EnvStatus.WARNING, capturedEvent().previousStatus()),
                () -> assertEquals(EnvStatus.NORMAL, capturedEvent().currentStatus())
        );
    }

    @Test
    @DisplayName("임계 시간이 없는경우 즉시 CRITICAL로 전이")
    void criticalMessage() {
        when(repository.find(anyString(), anyString())).thenReturn(Optional.empty());

        node.process(messageOf(createRuleResult(true, null, at(0))));

        EnvironmentDecisionState saved = capturedState();
        assertEquals(EnvStatus.CRITICAL, saved.state());
        assertEquals(BASE, saved.lastMeasuredAt());

        EnvironmentEventDecisionDto event = capturedEvent();
        assertEquals(EnvStatus.NORMAL, event.previousStatus());
        assertEquals(EnvStatus.CRITICAL, event.currentStatus());
        assertEquals(EnvironmentEventReason.STATUS_CHANGED, event.reason());
    }

    @Test
    @DisplayName("알림 주기 이내면 lastMeasuredAt만 갱신하고 알림을 보내지 않는다")
    void withinAlertInterval() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.CRITICAL, BASE, BASE, BASE)));

        Instant measuredAt = BASE.plus(Duration.ofSeconds(30));
        node.process(messageOf(createRuleResult(true, 0, measuredAt.toString())));

        EnvironmentDecisionState saved = capturedState();

        assertAll(
                () -> assertEquals(EnvStatus.CRITICAL, saved.state()),
                () -> assertEquals(BASE, saved.lastAlertAt()),
                () -> assertEquals(measuredAt, saved.lastMeasuredAt())
        );
        verify(connection, never()).deliver(any(Message.class));
    }

    @Test
    @DisplayName("임계 시간있는경우 최초 초과시 WARNING으로 변경")
    void firstThresholdExceeded() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.WARNING, BASE, null, BASE)));

        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, at(5))));

        assertAll(
                () -> assertEquals(EnvStatus.WARNING, capturedState().state()),
                () -> assertEquals(BASE.plus(Duration.ofMinutes(5)), capturedState().lastMeasuredAt())
        );

        verify(connection, never()).deliver(any(Message.class));
    }


    @Test
    @DisplayName("지속시간이 임계시간 미만인경우엔 이벤트 발생하지 않음")
    void warningNotCritical() {
        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.WARNING, BASE, null, BASE)));

        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, at(5))));

        EnvironmentDecisionState state = capturedState();

        assertAll(
                () -> assertEquals(EnvStatus.WARNING, state.state()),
                () -> assertEquals(BASE.plus(Duration.ofMinutes(5)), state.lastMeasuredAt())
        );

        verify(connection, never()).deliver(any(Message.class));

    }

    @Test
    @DisplayName("지속지간 지난경우 CRITICAL로 변경")
    void durationExceeded() {

        when(repository.find(anyString(), anyString()))
                .thenReturn(Optional.of(state(EnvStatus.WARNING, BASE, null, BASE)));

        node.process(messageOf(createRuleResult(true, DURATION_MINUTES, at(30))));

        EnvironmentDecisionState state = capturedState();
        EnvironmentEventDecisionDto event = capturedEvent();

        assertAll(
                () -> assertEquals(EnvStatus.CRITICAL, state.state()),
                () -> assertEquals(BASE.plus(Duration.ofMinutes(30)), state.lastMeasuredAt()),
                () -> assertEquals(EnvironmentEventReason.STATUS_CHANGED, event.reason())
        );

        verify(connection, times(1)).deliver(any(Message.class));

    }

    private EnvironmentDecisionState capturedState() {
        ArgumentCaptor<EnvironmentDecisionState> captor =
                ArgumentCaptor.forClass(EnvironmentDecisionState.class);
        verify(repository).save(anyString(), anyString(), captor.capture());
        return captor.getValue();
    }


    private Message capturedMessage() {
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(connection).deliver(captor.capture());
        return captor.getValue();
    }

    private EnvironmentEventDecisionDto capturedEvent() {
        return capturedMessage().get(MessageFields.ENVIRONMENT_EVENT_DECISION);
    }

    private static String at(long plusMinutes) {
        return BASE.plus(Duration.ofMinutes(plusMinutes)).toString();
    }

    private static EnvironmentDecisionState state(
            EnvStatus status,
            Instant firstViolatedAt,
            Instant lastAlertAt,
            Instant lastMeasuredAt
    ) {
        return state(status, firstViolatedAt, lastAlertAt, null, lastMeasuredAt);
    }

    private static EnvironmentDecisionState state(
            EnvStatus status,
            Instant firstViolatedAt,
            Instant lastAlertAt,
            Instant firstNormalAt,
            Instant lastMeasuredAt
    ) {
        return new EnvironmentDecisionState(status, firstViolatedAt, lastAlertAt, firstNormalAt, lastMeasuredAt);
    }


    private Message messageOf(RuleResultDto ruleResult) {
        return new Message(Map.of(MessageFields.RULE_RESULT, ruleResult));
    }

    private RuleResultDto createRuleResult(
            boolean violated,
            Integer durationMinutes,
            String measuredAt
    ) {
        return new RuleResultDto(
                1L,
                "device-eui",
                1L,
                1L,
                "TEMPERATURE",
                violated
                        ? ViolationType.ABOVE_MAX
                        : ViolationType.NORMAL,
                violated,
                30.0,
                20.0,
                25.0,
                "°C",
                measuredAt,
                durationMinutes,
                violated ? "온도 초과" : null
        );
    }

}