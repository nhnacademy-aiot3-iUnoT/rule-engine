package com.nhnacademy.ruleengine.engine.core;

import java.util.function.Supplier;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

// 모든 Flow의 등록, 시작, 중지를 공통으로 처리한다.
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowLifecycleManager {

    private final FlowEngine flowEngine;

    public boolean isRegistered(String flowId) {
        return flowEngine.getFlows().containsKey(flowId);
    }

    public void start(String flowId, Supplier<Flow> flowSupplier) {
        if (flowEngine.getFlows().containsKey(flowId)) {
            flowEngine.startFlow(flowId);
        } else {
            flowEngine.registerAndStart(flowSupplier.get());
        }

        log.info("Flow를 시작했습니다. flowId={}", flowId);
    }

    public void stop(String flowId) {
        if (!isRegistered(flowId)) {
            return;
        }

        flowEngine.stopFlow(flowId);
        log.info("Flow를 중지했습니다. flowId={}", flowId);
    }
}
