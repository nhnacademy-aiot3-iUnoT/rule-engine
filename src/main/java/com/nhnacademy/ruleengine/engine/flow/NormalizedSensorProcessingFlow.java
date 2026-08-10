package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.command.rule.EnvironmentRuleCommand;
import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.impl.*;
import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import com.nhnacademy.ruleengine.engine.service.NotificationPreferenceService;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

// 정규화 센서 메시지의 검증과 저장, 룰 판단, 상태전이, 이벤트 생성 파이프라인을 구성한다.
@Component
@RequiredArgsConstructor
public class NormalizedSensorProcessingFlow {

    public static final String FLOW_ID = "normalized-sensor-processing-flow";

    private static final String VALIDATION_NODE_ID = "sensor-payload-validation";
    private static final String DATABASE_SAVE_NODE_ID = "sensor-database-save";
    private static final String RULE_EVALUATION_NODE_ID = "flow-rule-evaluation";
    private static final String ENVIRONMENT_STATUS_NODE_ID = "flow-environment-status";
    private static final String EVENT_CREATE_NODE_ID = "flow-event-create";
    private static final String NOTIFICATION_NODE_ID = "flow-notification";

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final NormalizedSensorConsumerNode normalizedSensorConsumer;
    private final SensorInfluxService sensorInfluxService;
    private final List<EnvironmentRuleCommand> environmentRuleCommands;
    private final EnvironmentDecisionStateRedisRepository environmentDecisionStateRedisRepository;
    private final NotificationPreferenceService notificationPreferenceService;
    private final List<NotificationSender> senders;

    public Flow create() {
        return new Flow(FLOW_ID)
                .addNode(normalizedSensorConsumer)
                .addNode(new SensorPayloadValidationNode(
                        VALIDATION_NODE_ID
                ))
                .addNode(new DatabaseSaveNode(
                        DATABASE_SAVE_NODE_ID,
                        sensorInfluxService
                ))
                .addNode(new EnvironmentRuleEvaluationNode(
                        RULE_EVALUATION_NODE_ID,
                        environmentRuleCommands
                ))
                .addNode(new EnvironmentStatusDecisionNode(
                        ENVIRONMENT_STATUS_NODE_ID,
                        environmentDecisionStateRedisRepository
                ))
                .addNode(new EventCreateNode(
                        EVENT_CREATE_NODE_ID
                ))
                .addNode(new NotificationDispatchNode(
                        NOTIFICATION_NODE_ID,
                        notificationPreferenceService,
                        senders
                ))
                .connect(
                        NormalizedSensorConsumerNode.NODE_ID,
                        OUTPUT_PORT,
                        VALIDATION_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        VALIDATION_NODE_ID,
                        OUTPUT_PORT,
                        DATABASE_SAVE_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        DATABASE_SAVE_NODE_ID,
                        OUTPUT_PORT,
                        RULE_EVALUATION_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        RULE_EVALUATION_NODE_ID,
                        OUTPUT_PORT,
                        ENVIRONMENT_STATUS_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        ENVIRONMENT_STATUS_NODE_ID,
                        OUTPUT_PORT,
                        EVENT_CREATE_NODE_ID,
                        INPUT_PORT
                )
                .connect(
                        EVENT_CREATE_NODE_ID,
                        OUTPUT_PORT,
                        NOTIFICATION_NODE_ID,
                        INPUT_PORT
                );
    }
}
