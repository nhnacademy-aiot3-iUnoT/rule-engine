package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.engine.flow.ExternalSensorFlow;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import org.springframework.stereotype.Component;

import java.util.Set;

// 외부 MQTT 수집 Flow를 클러스터에서 한 인스턴스만 실행하도록 조정한다.
// 브로커에 중복 구독이 생기면 같은 원본 데이터가 두 번 수집되므로 단일 소유가 필수다.
@Component
public class ExternalIngressCoordinator extends LeasedFlowOwner<String> {

    // 소유 단위가 하나뿐이라 목표 상태는 항상 고정이다.
    private static final Set<String> SINGLE_KEY = Set.of(ExternalSensorFlow.FLOW_ID);

    private final ExternalSensorFlow externalSensorFlow;
    private final String lockKey;

    public ExternalIngressCoordinator(
            RedisLeaseLockService redisLeaseLockService,
            RedundancyProperties redundancyProperties,
            FlowEngine flowEngine,
            ExternalSensorFlow externalSensorFlow
    ) {
        super(
                redisLeaseLockService,
                flowEngine,
                redundancyProperties.instanceId(),
                redundancyProperties.externalIngress()
        );

        this.externalSensorFlow = externalSensorFlow;
        this.lockKey = redundancyProperties.externalIngress().lockKey();
    }

    @Override
    protected Set<String> desiredKeys() {
        return SINGLE_KEY;
    }

    @Override
    protected String lockKey(String key) {
        return lockKey;
    }

    @Override
    protected String flowId(String key) {
        return key;
    }

    @Override
    protected Flow createFlow(String key) {
        return externalSensorFlow.create();
    }
}
