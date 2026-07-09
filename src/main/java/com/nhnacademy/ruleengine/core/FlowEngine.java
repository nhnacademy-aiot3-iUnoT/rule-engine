package com.nhnacademy.ruleengine.core;

import com.nhnacademy.ruleengine.core.connection.Connection;
import com.nhnacademy.ruleengine.core.node.AbstractNode;
import com.nhnacademy.ruleengine.core.node.Node;
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
        flows.put(flow.getId(), flow);
        log.info("[Engine] 플로우 '{}' 등록됨", flow.getId());
    }

    public void startFlow(String flowId) {
        Flow flow = getFlowOrThrow(flowId);
        List<String> errors = flow.validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException("Flow '" + flowId + "' is not valid: " + errors);
        }

        stopConnectionTasks(flowId);
        for (Connection connection : flow.getConnections()) {
            startConnection(flowId, connection);
        }
        flow.initialize();

        flowEngineState = FlowEngineState.RUNNING;
        log.info("[Engine] 플로우 '{}' 시작됨", flow.getId());
    }

    public void startConnection(String flowId, Connection connection) {
        if (!flows.containsKey(flowId)) {
            throw new IllegalArgumentException("Flow '" + flowId + "' not found");
        }

        Map<String, Future<?>> flowTasks = connectionTasks.computeIfAbsent(flowId, id -> new ConcurrentHashMap<>());
        String taskId = Flow.normalizeConnectionId(connection.getId());

        if (flowTasks.containsKey(taskId) && !flowTasks.get(taskId).isDone()) {
            return;
        }

        Future<?> future = executorService.submit(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Message message = connection.poll();
                    if (message != null && connection.getTarget() != null) {
                        connection.getTarget().receive(message);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("[Engine] connection task failed. connectionId={}", connection.getId(), e);
                }
            }
        });
        flowTasks.put(taskId, future);
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
        Flow flow = flows.get(flowId);
        if (flow != null) {
            stopConnectionTasks(flowId);
            flow.shutdown();
            log.info("[Engine] 플로우 '{}' 정지됨", flowId);
        }
    }

    public void shutdown() {
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
        Map<String, Future<?>> flowTasks = connectionTasks.remove(flowId);
        if (flowTasks == null) {
            return;
        }

        for (Future<?> future : flowTasks.values()) {
            future.cancel(true);
        }
    }

}
