package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.FlowLifecycleManager;
import com.nhnacademy.ruleengine.engine.flow.NormalizedSensorProcessingFlow;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

// Rabbit Listener가 시작되기 전에 정규화 센서 처리 Flow를 등록한다.
@Component
@RequiredArgsConstructor
public class NormalizedSensorFlowInitializer implements SmartInitializingSingleton {

    private final FlowLifecycleManager flowLifecycleManager;
    private final NormalizedSensorProcessingFlow normalizedSensorProcessingFlow;

    @Override
    public void afterSingletonsInstantiated() {
        flowLifecycleManager.start(
                NormalizedSensorProcessingFlow.FLOW_ID,
                normalizedSensorProcessingFlow::create
        );
    }
}
