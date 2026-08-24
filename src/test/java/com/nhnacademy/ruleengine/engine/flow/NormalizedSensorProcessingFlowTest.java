package com.nhnacademy.ruleengine.engine.flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.impl.*;
import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import com.nhnacademy.ruleengine.engine.repository.SensorDailyStatRedisRepository;
import com.nhnacademy.ruleengine.engine.service.NotificationPreferenceService;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.engine.service.ZoneEnvStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class NormalizedSensorProcessingFlowTest {

    // 처리 순서대로 나열한 노드 ID. Flow가 이 순서대로 이어야 한다.
    private static final List<String> PIPELINE_NODE_IDS = List.of(
            NormalizedSensorConsumerNode.NODE_ID,
            "sensor-payload-validation",
            "sensor-database-save",
            "flow-rule-evaluation",
            "flow-daily-threshold-stat",
            "flow-environment-status",
            "flow-event-create",
            "flow-zone-env-status",
            "flow-notification"
    );

    @Mock
    private SensorInfluxService sensorInfluxService;

    @Mock
    private EnvironmentDecisionStateRedisRepository environmentDecisionStateRedisRepository;

    @Mock
    private SensorDailyStatRedisRepository sensorDailyStatRedisRepository;

    @Mock
    private ZoneEnvStatusService zoneEnvStatusService;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @Mock
    private NotificationSender notificationSender;

    private NormalizedSensorConsumerNode consumerNode;
    private NormalizedSensorProcessingFlow processingFlow;

    @BeforeEach
    void setUp() {
        consumerNode = new NormalizedSensorConsumerNode(new ObjectMapper());

        processingFlow = new NormalizedSensorProcessingFlow(
                consumerNode,
                sensorInfluxService,
                List.of(),
                environmentDecisionStateRedisRepository,
                sensorDailyStatRedisRepository,
                zoneEnvStatusService,
                notificationPreferenceService,
                List.of(notificationSender)
        );
    }

    @Test
    @DisplayName("파이프라인 노드를 모두 등록한다")
    void createNodes() {
        Flow flow = processingFlow.create();

        assertAll(
                () -> assertEquals(NormalizedSensorProcessingFlow.FLOW_ID, flow.getId()),
                () -> assertEquals(PIPELINE_NODE_IDS.size(), flow.getNodes().size()),
                () -> assertTrue(flow.getNodes().keySet().containsAll(PIPELINE_NODE_IDS))
        );
    }

    @Test
    @DisplayName("노드 타입이 처리 순서와 일치한다")
    void nodeTypes() {
        Flow flow = processingFlow.create();

        assertAll(
                () -> assertInstanceOf(NormalizedSensorConsumerNode.class,
                        flow.getNodes().get(NormalizedSensorConsumerNode.NODE_ID)),
                () -> assertInstanceOf(SensorPayloadValidationNode.class,
                        flow.getNodes().get("sensor-payload-validation")),
                () -> assertInstanceOf(DatabaseSaveNode.class,
                        flow.getNodes().get("sensor-database-save")),
                () -> assertInstanceOf(EnvironmentRuleEvaluationNode.class,
                        flow.getNodes().get("flow-rule-evaluation")),
                () -> assertInstanceOf(DailyThresholdStatNode.class,
                        flow.getNodes().get("flow-daily-threshold-stat")),
                () -> assertInstanceOf(EnvironmentStatusDecisionNode.class,
                        flow.getNodes().get("flow-environment-status")),
                () -> assertInstanceOf(EventCreateNode.class,
                        flow.getNodes().get("flow-event-create")),
                () -> assertInstanceOf(ZoneEnvStatusReportNode.class,
                        flow.getNodes().get("flow-zone-env-status")),
                () -> assertInstanceOf(NotificationDispatchNode.class,
                        flow.getNodes().get("flow-notification"))
        );
    }

    @Test
    @DisplayName("앞 노드의 out을 다음 노드의 in으로 차례대로 잇는다")
    void connectInOrder() {
        Flow flow = processingFlow.create();

        assertEquals(PIPELINE_NODE_IDS.size() - 1, flow.getConnections().size());

        for (int i = 0; i < PIPELINE_NODE_IDS.size() - 1; i++) {
            String connectionId = PIPELINE_NODE_IDS.get(i) + ":out->" + PIPELINE_NODE_IDS.get(i + 1) + ":in";

            assertNotNull(flow.getConnection(connectionId), connectionId);
        }
    }

    @Test
    @DisplayName("마지막 노드는 다음으로 잇지 않는다")
    void lastNodeHasNoOutgoingConnection() {
        Flow flow = processingFlow.create();

        List<Connection> connections = flow.getConnectionsOfNode("flow-notification");

        assertAll(
                () -> assertEquals(1, connections.size()),
                () -> assertTrue(connections.get(0).getId().endsWith("flow-notification:in"))
        );
    }

    @Test
    @DisplayName("배선에 문제가 없어 검증을 통과한다")
    void validate() {
        assertTrue(processingFlow.create().validate().isEmpty());
    }

    @Test
    @DisplayName("RabbitMQ 수신 노드는 주입받은 빈을 그대로 쓴다")
    void reusesInjectedConsumerNode() {
        // @RabbitListener가 붙은 빈이라 새로 만들면 메시지를 받지 못한다.
        assertSame(
                consumerNode,
                processingFlow.create().getNodes().get(NormalizedSensorConsumerNode.NODE_ID)
        );
    }
}
