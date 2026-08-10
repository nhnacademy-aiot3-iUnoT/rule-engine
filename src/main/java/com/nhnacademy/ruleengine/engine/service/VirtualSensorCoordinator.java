package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.flow.VirtualSensorFlow;
import com.nhnacademy.ruleengine.engine.repository.VirtualSensorRedisRepository;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class VirtualSensorCoordinator extends LeasedFlowOwner<Long> {

    private final VirtualSensorRedisRepository virtualSensorRedisRepository;
    private final VirtualSensorFlow virtualSensorFlow;
    private final String lockKeyPrefix;

    // 현재 Flow가 실행 중인 설정. 최신 설정과 달라지면 Flow를 재시작해 무중단으로 반영한다.
    private final Map<Long, VirtualSensorConfig> runningConfigs = new HashMap<>();

    public VirtualSensorCoordinator(
            VirtualSensorRedisRepository virtualSensorRedisRepository,
            RedisLeaseLockService redisLeaseLockService,
            RedundancyProperties redundancyProperties,
            FlowEngine flowEngine,
            VirtualSensorFlow virtualSensorFlow
    ) {
        super(
                redisLeaseLockService,
                flowEngine,
                redundancyProperties.instanceId(),
                redundancyProperties.virtualSensor()
        );

        this.virtualSensorRedisRepository = virtualSensorRedisRepository;
        this.virtualSensorFlow = virtualSensorFlow;
        this.lockKeyPrefix = redundancyProperties.virtualSensor().lockKey();
    }

    @Override
    protected Set<Long> desiredKeys() {
        return virtualSensorRedisRepository.findAllActiveSectionIds();
    }

    @Override
    protected String lockKey(Long sectionId) {
        return lockKeyPrefix + ":" + sectionId;
    }

    @Override
    protected String flowId(Long sectionId) {
        return VirtualSensorFlow.flowId(sectionId);
    }

    @Override
    protected Flow createFlow(Long sectionId) {
        VirtualSensorConfig config = virtualSensorRedisRepository
                .getVirtualSensorConfig(sectionId)
                .orElseThrow(() -> new IllegalStateException(
                        "가상 센서 설정이 없습니다. sectionId=" + sectionId
                ));

        Flow flow = virtualSensorFlow.create(config);
        runningConfigs.put(sectionId, config);

        return flow;
    }

    // 설정이 변경된 경우에만 Flow를 다시 시작해 최신 설정을 반영한다.
    @Override
    protected void onLeaseRenewed(Long sectionId) {
        VirtualSensorConfig latestConfig = virtualSensorRedisRepository
                .getVirtualSensorConfig(sectionId)
                .orElse(null);

        if (latestConfig == null || latestConfig.equals(runningConfigs.get(sectionId))) {
            return;
        }

        log.info("가상 센서 설정이 변경되어 Flow를 재시작합니다. sectionId={}", sectionId);

        // createFlow()가 최신 설정을 다시 읽어 runningConfigs까지 갱신한다.
        restartFlow(sectionId);
    }

    @Override
    protected void onOwnershipReleased(Long sectionId) {
        runningConfigs.remove(sectionId);
    }
}
