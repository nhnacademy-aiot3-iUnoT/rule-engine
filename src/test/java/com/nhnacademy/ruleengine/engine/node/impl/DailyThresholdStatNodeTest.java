package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.connection.impl.LocalConnection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.repository.SensorDailyStatRedisRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Month;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DailyThresholdStatNodeTest {

    @Mock
    private SensorDailyStatRedisRepository sensorDailyStatRedisRepository;

    private DailyThresholdStatNode node;

    @BeforeEach
    void setUp() {
        node = new DailyThresholdStatNode("daily-threshold-stat", sensorDailyStatRedisRepository);
    }

    @Test
    @DisplayName("판단에 쓰인 임계값과 이탈 여부를 측정 시각의 날짜로 기록한다")
    void recordsWithAppliedThreshold() {
        LocalConnection connection = connect();

        node.process(messageWith(ViolationType.ABOVE_MAX, true, 10.0, 30.0));

        assertAll(
                () -> verify(sensorDailyStatRedisRepository).recordDailyStat(
                        3L,
                        LocalDate.of(2026, Month.AUGUST, 17),
                        "temperature",
                        true,
                        10.0,
                        30.0
                ),
                () -> assertEquals(1, connection.getBufferSize())
        );
    }

    @Test
    @DisplayName("측정 시각이 KST 자정 이전이면 전날로 센다")
    void countsIntoKoreanDay() {
        connect();

        // 2026-08-17T14:30Z 는 KST로 2026-08-17 23:30이라 같은 날이다.
        node.process(messageWith(
                ViolationType.NORMAL, false, 10.0, 30.0, "2026-08-17T14:30:00Z"
        ));

        verify(sensorDailyStatRedisRepository).recordDailyStat(
                anyLong(), any(LocalDate.class), anyString(), anyBoolean(), any(), any()
        );
    }

    @Test
    @DisplayName("임계값이 없는 판단은 세지 않고 그대로 넘긴다")
    void skipsWithoutThreshold() {
        LocalConnection connection = connect();

        node.process(messageWith(ViolationType.NORMAL, false, null, null));

        assertAll(
                () -> verify(sensorDailyStatRedisRepository, never()).recordDailyStat(
                        any(), any(), any(), anyBoolean(), any(), any()
                ),
                () -> assertEquals(1, connection.getBufferSize())
        );
    }

    @Test
    @DisplayName("통계 기록이 실패해도 다음 노드로 메시지를 넘긴다")
    void continuesWhenRecordFails() {
        LocalConnection connection = connect();

        doThrow(new IllegalStateException("redis 장애"))
                .when(sensorDailyStatRedisRepository)
                .recordDailyStat(any(), any(), any(), anyBoolean(), any(), any());

        assertDoesNotThrow(() -> node.process(messageWith(ViolationType.NORMAL, false, 10.0, 30.0)));

        assertEquals(1, connection.getBufferSize());
    }

    @Test
    @DisplayName("ruleResult가 없으면 세지 않고 리턴한다")
    void stopsWhenRuleResultIsMissing() {
        LocalConnection connection = connect();

        assertDoesNotThrow(() -> node.process(new Message(Map.of())));

        assertAll(
                () -> verify(sensorDailyStatRedisRepository, never()).recordDailyStat(
                        any(), any(), any(), anyBoolean(), any(), any()
                ),
                () -> assertEquals(0, connection.getBufferSize())
        );
    }

    private LocalConnection connect() {
        LocalConnection connection = new LocalConnection("out-connection");
        node.getOutputPort("out").connect(connection);

        return connection;
    }

    private Message messageWith(
            ViolationType violationType,
            boolean violated,
            Double min,
            Double max
    ) {
        return messageWith(violationType, violated, min, max, "2026-08-17T05:00:00Z");
    }

    private Message messageWith(
            ViolationType violationType,
            boolean violated,
            Double min,
            Double max,
            String measuredAt
    ) {
        return new Message(Map.of(
                MessageFields.RULE_RESULT,
                new RuleResultDto(
                        1L,
                        "device-eui",
                        2L,
                        3L,
                        "temperature",
                        violationType,
                        violated,
                        31.5,
                        min,
                        max,
                        "C",
                        measuredAt,
                        10,
                        "temperature: 최댓값 초과"
                )
        ));
    }
}
