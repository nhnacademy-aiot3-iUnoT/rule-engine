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
import java.util.stream.Collectors;

@Slf4j
@Component
public class VirtualSensorCoordinator extends LeasedFlowOwner<String> {

    private final VirtualSensorRedisRepository virtualSensorRedisRepository;
    private final VirtualSensorFlow virtualSensorFlow;
    private final ZoneResolver zoneResolver;
    private final String lockKeyPrefix;

    // 현재 Flow가 실행 중인 설정. 최신 설정과 달라지면 Flow를 재시작해 무중단으로 반영한다.
    private final Map<String, VirtualSensorConfig> runningConfigs = new HashMap<>();

    public VirtualSensorCoordinator(
            VirtualSensorRedisRepository virtualSensorRedisRepository,
            RedisLeaseLockService redisLeaseLockService,
            RedundancyProperties redundancyProperties,
            FlowEngine flowEngine,
            VirtualSensorFlow virtualSensorFlow,
            ZoneResolver zoneResolver
    ) {
        super(
                redisLeaseLockService,
                flowEngine,
                redundancyProperties.instanceId(),
                redundancyProperties.virtualSensor()
        );

        this.virtualSensorRedisRepository = virtualSensorRedisRepository;
        this.virtualSensorFlow = virtualSensorFlow;
        this.zoneResolver = zoneResolver;
        this.lockKeyPrefix = redundancyProperties.virtualSensor().lockKey();
    }

    @Override
    protected Set<String> desiredKeys() {
        return virtualSensorRedisRepository.findAllActiveDeviceEuis()
                .stream()
                .filter(this::zoneAcceptsData)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * 등록된 구역이나 그 저장소가 비활성이면 데이터를 만들어도 버려지므로 Flow 자체를 내린다.
     * 다시 활성으로 바꾸면 다음 주기에 저절로 살아난다.
     * <p>
     * 아직 구역에 등록하지 않은 가상 센서는 그대로 둔다. 등록되는 순간 바로 흐르게 하기 위해서다.
     */
    private boolean zoneAcceptsData(String deviceEui) {
        return zoneResolver.resolve(deviceEui)
                .map(zone -> {
                    if (zoneResolver.isZoneActive(zone.zoneId())) {
                        return true;
                    }

                    log.debug(
                            "비활성 구역의 가상 센서라 Flow를 실행하지 않습니다. deviceEui={}, zoneId={}",
                            deviceEui,
                            zone.zoneId()
                    );

                    return false;
                })
                .orElse(true);
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
