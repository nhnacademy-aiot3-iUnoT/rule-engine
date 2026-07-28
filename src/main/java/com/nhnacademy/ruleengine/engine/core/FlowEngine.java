package com.nhnacademy.ruleengine.engine.core;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
// 여러 Flow와 Connection 처리 작업을 등록·실행·종료한다.
public class FlowEngine {

    public enum FlowEngineState {
        INITIALIZED,
        RUNNING,
        STOPPED
    }

    private final Map<String, Flow> flows = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Future<?>>> connectionTasks =
            new ConcurrentHashMap<>();
    private final ExecutorService executorService =
            Executors.newCachedThreadPool();

    @Getter
    private volatile FlowEngineState flowEngineState =
            FlowEngineState.INITIALIZED;

    public boolean containsFlow(String flowId) {
        return flows.containsKey(flowId);
    }

    public void register(Flow flow) {
        // Flow ID를 기준으로 실행 대상 Flow를 등록한다.
        flows.putIfAbsent(flow.getId(), flow);
        log.debug("[Engine] 플로우 '{}' 등록됨", flow.getId());
    }

    public void registerAndStart(Flow flow) {
        register(flow);
        startFlow(flow.getId());
    }

    public void startFlow(String flowId) {
        // 유효성 검사를 통과한 Flow만 초기화하고 실행한다.
        Flow flow = getFlowOrThrow(flowId);

        List<String> errors = flow.validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Flow '" + flowId + "' is not valid: " + errors);
        }

        stopConnectionTasks(flowId);

        flow.initialize();
        for (Connection connection : flow.getConnections()) {
            startConnection(flowId, connection);
        }

        flowEngineState = FlowEngineState.RUNNING;
        log.debug("[Engine] 플로우 '{}' 시작됨", flow.getId());
    }

    private void startConnection(String flowId, Connection connection) {
        // Connection마다 메시지를 소비하는 별도 작업을 실행한다.
        getFlowOrThrow(flowId);

        Map<String, Future<?>> flowTasks = connectionTasks.computeIfAbsent(
                flowId,
                id -> new ConcurrentHashMap<>()
        );
        String taskId = Flow.normalizeConnectionId(connection.getId());

        if (isTaskRunning(flowTasks.get(taskId))) {
            return;
        }

        Future<?> future = executorService.submit(
                () -> consumeMessages(connection)
        );
        flowTasks.put(taskId, future);
    }

    private boolean isTaskRunning(Future<?> task) {
        return task != null && !task.isDone();
    }

    private void consumeMessages(Connection connection) {
        // 중단 요청 전까지 버퍼의 메시지를 대상 입력 포트로 전달한다.
        while (!Thread.currentThread().isInterrupted()) {
            Message message = null;

            try {
                message = connection.poll();
                deliverMessage(connection, message);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                handleConnectionFailure(connection, message, exception);
            }
        }
    }

    private void deliverMessage(
            Connection connection,
            Message message
    ) {
        if (message == null || connection.getTarget() == null) {
            return;
        }

        connection.getTarget().receive(message);
    }

    private void handleConnectionFailure(
            Connection connection,
            Message message,
            Exception exception
    ) {
        completeMessageExceptionally(message, exception);

        if (exception instanceof IllegalArgumentException) {
            log.debug(
                    "[Engine] 잘못된 메시지를 거절했습니다. connectionId={}, reason={}",
                    connection.getId(),
                    exception.getMessage()
            );
            return;
        }

        log.error(
                "[Engine] connection task failed. connectionId={}",
                connection.getId(),
                exception
        );
    }

    private void completeMessageExceptionally(
            Message message,
            Exception exception
    ) {
        if (message == null) {
            return;
        }

        FlowProcessingCompletion completion = message.get(
                MessageFields.FLOW_PROCESSING_COMPLETION
        );

        if (completion != null) {
            completion.completeExceptionally(exception);
        }
    }

    public void stopFlow(String flowId) {
        // 소비 작업을 먼저 취소한 뒤 Flow 자원을 종료한다.
        Flow flow = flows.get(flowId);
        if (flow != null) {
            stopConnectionTasks(flowId);
            flow.shutdown();
            log.debug("[Engine] 플로우 '{}' 정지됨", flowId);
        }
    }

    public void shutdown() {
        // 등록된 모든 작업과 Flow를 종료하고 Executor를 정리한다.
        for (String flowId : flows.keySet()) {
            stopConnectionTasks(flowId);
        }
        for (Flow flow : flows.values()) {
            flow.shutdown();
        }
        executorService.shutdownNow();
        flowEngineState = FlowEngineState.STOPPED;
    }

    private Flow getFlowOrThrow(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow == null) {
            throw new IllegalArgumentException("Flow '" + flowId + "' not found");
        }
        return flow;
    }

    private void stopConnectionTasks(String flowId) {
        // 해당 Flow에 속한 모든 비동기 소비 작업을 취소한다.
        Map<String, Future<?>> flowTasks = connectionTasks.remove(flowId);
        if (flowTasks == null) {
            return;
        }

        for (Future<?> future : flowTasks.values()) {
            future.cancel(true);
        }
    }

}
