package com.nhnacademy.ruleengine.engine.core;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
// Flow를 등록하고 Connection 작업을 실행·중지한다.
public class FlowEngine {

    private final Map<String, Flow> flows = new HashMap<>();
    private final Map<String, List<Future<?>>> connectionTasks = new HashMap<>();
    private final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();

    // Flow가 없으면 등록하고, 중지 상태면 시작한다.
    public synchronized void ensureStarted(Flow candidate) {
        Flow flow = Objects.requireNonNull(
                candidate,
                "Flow는 필수입니다."
        );

        Flow registeredFlow = flows.putIfAbsent(flow.getId(), flow);

        if (registeredFlow == null) {
            log.debug("[Engine] 플로우 '{}' 등록됨", flow.getId());
        } else {
            flow = registeredFlow;
        }

        if (flow.getFlowState() == Flow.FlowState.RUNNING) {
            return;
        }

        startFlow(flow);
    }

    // Flow 시작
    private void startFlow(Flow flow) {
        List<String> errors = flow.validate();
        if (!errors.isEmpty()) {
            throw new IllegalStateException(
                    "Flow '" + flow.getId() + "' is not valid: " + errors
            );
        }

        List<Future<?>> tasks = new ArrayList<>();
        try {
            flow.initialize();
            for (Connection connection : flow.getConnections()) {
                tasks.add(executorService.submit(
                        () -> consumeMessages(connection)
                ));
            }

            connectionTasks.put(flow.getId(), tasks);
            log.debug("[Engine] 플로우 '{}' 시작됨", flow.getId());
        } catch (RuntimeException exception) {
            cancelTasks(tasks);
            rollbackFlowInitialization(flow, exception);
            throw exception;
        }
    }

    private void consumeMessages(Connection connection) {
        while (!Thread.currentThread().isInterrupted()) {
            Message message = null;

            try {
                message = connection.poll();
                connection.getTarget().receive(message);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception exception) {
                handleConnectionFailure(connection, message, exception);
            }
        }
    }

    private void handleConnectionFailure(
            Connection connection,
            Message message,
            Exception exception
    ) {
        if (message != null) {
            message.completeProcessingExceptionally(exception);
        }

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

    public synchronized void stopFlow(String flowId) {
        Flow flow = flows.get(flowId);
        if (flow != null) {
            stopFlow(flow);
        }
    }

    public synchronized void stopAndRemoveFlow(String flowId) {
        stopFlow(flowId);
        flows.remove(flowId);
    }


    private void stopFlow(Flow flow) {
        List<Future<?>> tasks = connectionTasks.remove(flow.getId());
        cancelTasks(tasks);

        if (flow.getFlowState() == Flow.FlowState.RUNNING) {
            flow.shutdown();
            log.debug("[Engine] 플로우 '{}' 정지됨", flow.getId());
        }
    }

    private void cancelTasks(List<Future<?>> tasks) {
        if (tasks == null) {
            return;
        }

        tasks.forEach(task -> task.cancel(true));
    }

    // FlowEngine 종료
    public synchronized void shutdown() {
        for (Flow flow : flows.values()) {
            try {
                stopFlow(flow);
            } catch (RuntimeException exception) {
                log.error(
                        "[Engine] 플로우 종료에 실패했습니다. flowId={}",
                        flow.getId(),
                        exception
                );
            }
        }

        flows.clear();
        executorService.shutdownNow();
    }

    // Flow 생성 실패시 롤백
    private void rollbackFlowInitialization(
            Flow flow,
            RuntimeException startException
    ) {
        try {
            flow.shutdown();
        } catch (RuntimeException shutdownException) {
            startException.addSuppressed(shutdownException);
        }
    }

}
