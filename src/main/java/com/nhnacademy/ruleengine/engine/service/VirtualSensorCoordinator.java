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
public class VirtualSensorCoordinator extends LeasedFlowOwner<String> {

    private final VirtualSensorRedisRepository virtualSensorRedisRepository;
    private final VirtualSensorFlow virtualSensorFlow;
    private final String lockKeyPrefix;

    // 현재 Flow가 실행 중인 설정. 최신 설정과 달라지면 Flow를 재시작해 무중단으로 반영한다.
    private final Map<String, VirtualSensorConfig> runningConfigs = new HashMap<>();

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
    protected Set<String> desiredKeys() {
        return virtualSensorRedisRepository.findAllActiveDeviceEuis();
    }

    @Override
    protected String lockKey(String deviceEui) {
        return lockKeyPrefix + ":" + deviceEui;
    }

    @Override
    protected String flowId(String deviceEui) {
        return VirtualSensorFlow.flowId(deviceEui);
    }

    @Override
    protected Flow createFlow(String deviceEui) {
        VirtualSensorConfig config = virtualSensorRedisRepository
                .getVirtualSensorConfig(deviceEui)
                .orElseThrow(() -> new IllegalStateException(
                        "가상 센서 설정이 없습니다. deviceEui=" + deviceEui
                ));

        Flow flow = virtualSensorFlow.create(config);
        runningConfigs.put(deviceEui, config);

        return flow;
    }

    // 설정이 변경된 경우에만 Flow를 다시 시작해 최신 설정을 반영한다.
    @Override
    protected void onLeaseRenewed(String deviceEui) {
        VirtualSensorConfig latestConfig = virtualSensorRedisRepository
                .getVirtualSensorConfig(deviceEui)
                .orElse(null);

        if (latestConfig == null || latestConfig.equals(runningConfigs.get(deviceEui))) {
            return;
        }

        log.info("가상 센서 설정이 변경되어 Flow를 재시작합니다. deviceEui={}", deviceEui);

        // createFlow()가 최신 설정을 다시 읽어 runningConfigs까지 갱신한다.
        restartFlow(deviceEui);
    }

    @Override
    protected void onOwnershipReleased(String deviceEui) {
        runningConfigs.remove(deviceEui);
    }
}
