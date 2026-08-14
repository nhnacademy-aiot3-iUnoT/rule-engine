package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.command.rule.EnvironmentRuleCommand;
import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.node.impl.*;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import com.nhnacademy.ruleengine.engine.service.NotificationPreferenceService;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.engine.service.ZoneEnvStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// 정규화 센서 메시지의 검증과 저장, 룰 판단, 상태전이, 이벤트 생성, 구역 상태 반영, 알림 발송 파이프라인을 구성한다.
// 노드는 한 줄로 이어지므로, 아래 pipeline() 목록의 순서가 곧 처리 순서다.
@Component
@RequiredArgsConstructor
public class NormalizedSensorProcessingFlow {

    public static final String FLOW_ID = "normalized-sensor-processing-flow";

    private static final String VALIDATION_NODE_ID = "sensor-payload-validation";
    private static final String DATABASE_SAVE_NODE_ID = "sensor-database-save";
    private static final String RULE_EVALUATION_NODE_ID = "flow-rule-evaluation";
    private static final String ENVIRONMENT_STATUS_NODE_ID = "flow-environment-status";
    private static final String EVENT_CREATE_NODE_ID = "flow-event-create";
    private static final String ZONE_ENV_STATUS_NODE_ID = "flow-zone-env-status";
    private static final String NOTIFICATION_NODE_ID = "flow-notification";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final NormalizedSensorConsumerNode normalizedSensorConsumer;
    private final SensorInfluxService sensorInfluxService;
    private final List<EnvironmentRuleCommand> environmentRuleCommands;
    private final EnvironmentDecisionStateRedisRepository environmentDecisionStateRedisRepository;
    private final ZoneEnvStatusService zoneEnvStatusService;
    private final NotificationPreferenceService notificationPreferenceService;

    public Flow create() {
        Flow flow = new Flow(FLOW_ID);
        List<AbstractNode> pipeline = pipeline();

        pipeline.forEach(flow::addNode);
        connectInOrder(flow, pipeline);

        return flow;
    }

    // 처리 순서대로 나열한다. 순서를 바꾸려면 이 목록만 고치면 된다.
    private List<AbstractNode> pipeline() {
        return List.of(
                normalizedSensorConsumer,
                new SensorPayloadValidationNode(
                        VALIDATION_NODE_ID
                ),
                new DatabaseSaveNode(
                        DATABASE_SAVE_NODE_ID,
                        sensorInfluxService
                ),
                new EnvironmentRuleEvaluationNode(
                        RULE_EVALUATION_NODE_ID,
                        environmentRuleCommands
                ),
                new EnvironmentStatusDecisionNode(
                        ENVIRONMENT_STATUS_NODE_ID,
                        environmentDecisionStateRedisRepository
                ),
                new EventCreateNode(
                        EVENT_CREATE_NODE_ID
                ),
                new ZoneEnvStatusReportNode(
                        ZONE_ENV_STATUS_NODE_ID,
                        zoneEnvStatusService
                ),
                new NotificationDispatchNode(
                        NOTIFICATION_NODE_ID,
                        notificationPreferenceService
                )
        );
    }

    // 앞 노드의 출력 포트를 다음 노드의 입력 포트로 차례대로 잇는다.
    private void connectInOrder(Flow flow, List<AbstractNode> pipeline) {
        for (int i = 0; i < pipeline.size() - 1; i++) {
            flow.connect(
                    pipeline.get(i).getId(),
                    OUTPUT_PORT,
                    pipeline.get(i + 1).getId(),
                    INPUT_PORT
            );
        }
    }
}
