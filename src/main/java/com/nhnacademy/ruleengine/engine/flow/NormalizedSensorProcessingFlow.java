package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.impl.DatabaseSaveNode;
import com.nhnacademy.ruleengine.engine.node.impl.FlowCompletionNode;
import com.nhnacademy.ruleengine.engine.node.impl.SensorPayloadValidationNode;
import com.nhnacademy.ruleengine.engine.node.impl.NormalizedSensorConsumerNode;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 정규화 센서 메시지의 검증과 저장 파이프라인을 구성한다.
@Component
@RequiredArgsConstructor
public class NormalizedSensorProcessingFlow {

    public static final String FLOW_ID = "normalized-sensor-processing-flow";

    private static final String VALIDATION_NODE_ID = "sensor-payload-validation";
    private static final String DATABASE_SAVE_NODE_ID = "sensor-database-save";
    private static final String COMPLETION_NODE_ID = "flow-completion";
    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final NormalizedSensorConsumerNode normalizedSensorConsumer;
    private final SensorInfluxService sensorInfluxService;

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
                // 룰과 알림 Node는 DatabaseSaveNode와 CompletionNode 사이에 추가한다.
                .addNode(new FlowCompletionNode(
                        COMPLETION_NODE_ID
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
                        COMPLETION_NODE_ID,
                        INPUT_PORT
                );
    }
}
