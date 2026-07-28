package com.nhnacademy.ruleengine.engine.core;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.node.Node;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
// 여러 Flow와 Connection 처리 작업을 등록·실행·종료한다.
public class FlowEngine {

    public enum FlowEngineState {INITIALIZED, RUNNING, STOPPED}

    @Getter
    private final Map<String, Flow> flows;
    @Getter
    private FlowEngineState flowEngineState;
    private final ExecutorService executorService;
    private final Map<String, Map<String, Future<?>>> connectionTasks;

    public FlowEngine() {
        this.flowEngineState = FlowEngineState.INITIALIZED;
        this.flows = new ConcurrentHashMap<>();
        this.executorService = Executors.newCachedThreadPool();
        this.connectionTasks = new ConcurrentHashMap<>();
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

    public void startConnection(String flowId, Connection connection) {
        // Connection마다 메시지를 소비하는 별도 작업을 실행한다.
        if (!flows.containsKey(flowId)) {
            throw new IllegalArgumentException("Flow '" + flowId + "' not found");
        }

        Map<String, Future<?>> flowTasks = connectionTasks.computeIfAbsent(flowId, id -> new ConcurrentHashMap<>());
        String taskId = Flow.normalizeConnectionId(connection.getId());

        if (flowTasks.containsKey(taskId) && !flowTasks.get(taskId).isDone()) {
            return;
        }

        Future<?> future = executorService.submit(() -> {
            // 중단 요청 전까지 버퍼의 메시지를 대상 입력 포트로 전달한다.
            while (!Thread.currentThread().isInterrupted()) {
                Message message = null;
                try {
                    message = connection.poll();
                    if (message != null && connection.getTarget() != null) {
                        connection.getTarget().receive(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    completeMessageExceptionally(message, e);

                    if (e instanceof IllegalArgumentException) {
                        log.debug(
                                "[Engine] 잘못된 메시지를 거절했습니다. connectionId={}, reason={}",
                                connection.getId(),
                                e.getMessage()
                        );
                    } else {
                        log.error(
                                "[Engine] connection task failed. connectionId={}",
                                connection.getId(),
                                e
                        );
                    }
                }
            }
        });
        flowTasks.put(taskId, future);
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

    public void stopConnection(String flowId, String connectionId) {
        Map<String, Future<?>> flowTasks = connectionTasks.get(flowId);
        if (flowTasks == null) {
            return;
        }

        Future<?> future = flowTasks.remove(Flow.normalizeConnectionId(connectionId));
        if (future != null) {
            future.cancel(true);
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

    public List<Map<String, Object>> getRunningFlows() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Flow flow : flows.values()) {
            String status = "STOPPED";
            Map<String, Future<?>> tasks = connectionTasks.get(flow.getId());
            if (tasks != null && !tasks.isEmpty()) {
                status = "RUNNING";
            }

            result.add(Map.of(
                    "id", flow.getId(),
                    "name", flow.getId() != null ? flow.getId() : "이름 없음",
                    "status", status
            ));
        }
        return result;
    }

    public List<String> getNodeIdsByFlowId(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow == null) {
            return List.of();
        }

        List<String> nodeIds = new ArrayList<>();
        if (flow.getNodes() != null) {
            for (Node node : flow.getNodes().values()) {
                nodeIds.add(node.getId());
            }
        }
        return nodeIds;
    }

    public AbstractNode getNodeInstance(String nodeId) {
        for (Flow flow : flows.values()) {
            if (flow.getNodes() != null) {
                for (AbstractNode node : flow.getNodes().values()) {
                    if (nodeId.equals(node.getId())) {
                        return node;
                    }
                }
            }
        }
        return null;
    }

    public int getRunningFlowCount() {
        int activeCount = 0;
        for (String flowId : flows.keySet()) {
            Map<String, Future<?>> tasks = connectionTasks.get(flowId);
            if (tasks != null && !tasks.isEmpty()) {
                activeCount++;
            }
        }
        return activeCount;
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
