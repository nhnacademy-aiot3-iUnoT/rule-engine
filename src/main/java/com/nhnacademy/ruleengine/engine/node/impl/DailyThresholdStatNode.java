package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.ZoneDailySummary;
import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.repository.SensorDailyStatRedisRepository;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

/**
 * 임계값 판단 결과를 그 시점에 하루 단위로 세어 둔다.
 */
@Slf4j
public class DailyThresholdStatNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final SensorDailyStatRedisRepository sensorDailyStatRedisRepository;

    public DailyThresholdStatNode(
            String id,
            SensorDailyStatRedisRepository sensorDailyStatRedisRepository
    ) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
        this.sensorDailyStatRedisRepository = sensorDailyStatRedisRepository;
    }

    @Override
    protected void onProcess(Message message) {
        RuleResultDto ruleResult = message.get(MessageFields.RULE_RESULT);

        if (ruleResult == null) {
            log.error("[{}] ruleResult가 없습니다. 상위 노드의 payload 계약이 깨졌습니다.", getId());
            return;
        }

        // 통계는 수집 경로에 얹힌 부수 작업이다. 남기지 못했다고 측정값과 알림까지 잃으면 안 된다.
        try {
            record(ruleResult);
        } catch (RuntimeException exception) {
            log.warn(
                    "[{}] 임계값 판단 통계를 남기지 못했습니다. zoneId={}, sensorType={}",
                    getId(),
                    ruleResult.zoneId(),
                    ruleResult.sensorType(),
                    exception
            );
        }

        send(OUTPUT_PORT, message);
    }

    private void record(RuleResultDto ruleResult) {
        // 임계값이 없는 판단(door 등)은 이탈이라는 개념 자체가 없어 셀 대상이 아니다.
        if (ruleResult.min() == null && ruleResult.max() == null) {
            return;
        }

        sensorDailyStatRedisRepository.record(
                ruleResult.zoneId(),
                ZoneDailySummary.dateOf(resolveMeasuredAt(ruleResult)),
                ruleResult.sensorType(),
                ruleResult.violated(),
                ruleResult.min(),
                ruleResult.max()
        );
    }

    /**
     * 측정 시각을 하루 경계 판단에 쓸 수 있는 형태로 바꾼다.
     */
    private Instant resolveMeasuredAt(RuleResultDto ruleResult) {
        String measuredAt = ruleResult.measuredAt();

        if (measuredAt == null || measuredAt.isBlank()) {
            return Instant.now();
        }

        try {
            return Instant.parse(measuredAt.trim());
        } catch (RuntimeException exception) {
            log.warn(
                    "[{}] 측정 시각을 읽지 못해 현재 시각으로 셉니다. measuredAt={}",
                    getId(),
                    measuredAt
            );

            return Instant.now();
        }
    }
}
