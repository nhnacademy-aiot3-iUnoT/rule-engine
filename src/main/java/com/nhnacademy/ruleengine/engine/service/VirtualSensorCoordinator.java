package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
import com.nhnacademy.ruleengine.engine.flow.VirtualSensorFlow;
import com.nhnacademy.ruleengine.engine.repository.VirtualSensorRedisRepository;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// Redis의 활성 가상 센서 목록을 주기적으로 확인한다.
@Slf4j
@Component
public class VirtualSensorCoordinator implements SchedulingConfigurer {

    private final RedisLeaseLockService redisLeaseLockService;
    private final VirtualSensorRedisRepository virtualSensorRedisRepository;
    private final FlowEngine flowEngine;
    private final VirtualSensorFlow virtualSensorFlow;

    private final String ownerToken;
    private final String lockKeyPrefix;
    private final Duration leaseDuration;
    private final Duration checkInterval;
    private final Set<Long> ownedSectionIds = new HashSet<>();

    private boolean shutdownInProgress;

    public VirtualSensorCoordinator(
            VirtualSensorRedisRepository virtualSensorRedisRepository,
            RedisLeaseLockService redisLeaseLockService,
            RedundancyProperties redundancyProperties,
            FlowEngine flowEngine,
            VirtualSensorFlow virtualSensorFlow
    ) {
        this.virtualSensorRedisRepository = virtualSensorRedisRepository;
        this.redisLeaseLockService = redisLeaseLockService;
        this.flowEngine = flowEngine;
        this.virtualSensorFlow = virtualSensorFlow;

        RedundancyProperties.VirtualSensor virtualSensor =
                redundancyProperties.virtualSensor();

        this.ownerToken = redundancyProperties.instanceId()
                + ":" + UUID.randomUUID();
        this.lockKeyPrefix = virtualSensor.lockKeyPrefix();
        this.leaseDuration = virtualSensor.leaseDuration();
        this.checkInterval = virtualSensor.renewInterval();
    }

    // 설정된 주기로 활성 가상 센서 목록 확인 작업을 실행한다.
    @Override
    public void configureTasks(
            ScheduledTaskRegistrar taskRegistrar
    ) {
        taskRegistrar.addFixedDelayTask(
                this::checkActiveSections,
                checkInterval
        );
    }

    public synchronized void checkActiveSections() {
        if (shutdownInProgress) {
            return;
        }

        try {
            Set<Long> activeSectionIds =
                    virtualSensorRedisRepository
                            .findAllActiveSectionIds();

            renewOrReleaseOwnedLocks(activeSectionIds);
            acquireUnownedLocks(activeSectionIds);
        } catch (RuntimeException exception) {
            log.error(
                    "가상 센서 Lock 조정에 실패했습니다.",
                    exception
            );
        }
    }

    // 애플리케이션 종료 시 현재 인스턴스가 가진 Lock을 반환한다.
    @EventListener(ContextClosedEvent.class)
    public synchronized void handleApplicationShutdown() {
        if (shutdownInProgress) {
            return;
        }

        shutdownInProgress = true;

        for (Long sectionId : Set.copyOf(ownedSectionIds)) {
            stopFlowAndReleaseLock(sectionId);
        }

        ownedSectionIds.clear();
    }

    private void renewOrReleaseOwnedLocks(
            Set<Long> activeSectionIds
    ) {
        for (Long sectionId : Set.copyOf(ownedSectionIds)) {
            if (!activeSectionIds.contains(sectionId)) {
                stopFlowAndReleaseLock(sectionId);
                continue;
            }

            renewOwnedSectionLock(sectionId);
        }
    }

    private void acquireUnownedLocks(
            Set<Long> activeSectionIds
    ) {
        for (Long sectionId : activeSectionIds) {
            if (ownedSectionIds.contains(sectionId)) {
                continue;
            }

            acquireLockAndStartFlow(sectionId);
        }
    }

    // Lock 획득 성공 시 공통 Manager에 Flow 시작을 요청한다.
    private void acquireLockAndStartFlow(Long sectionId) {
        try {
            boolean acquired = redisLeaseLockService.acquire(
                    lockKey(sectionId),
                    ownerToken,
                    leaseDuration
            );

            if (!acquired) {
                log.debug(
                        "다른 인스턴스가 가상 센서 Lock을 소유하고 있습니다. sectionId={}",
                        sectionId
                );
                return;
            }

            VirtualSensorConfig config = virtualSensorRedisRepository
                    .getVirtualSensorConfig(sectionId)
                    .orElseThrow(() -> new IllegalStateException(
                            "가상 센서 설정이 없습니다. sectionId=" + sectionId
                    ));

            flowEngine.ensureStarted(
                    virtualSensorFlow.create(config)
            );

            ownedSectionIds.add(sectionId);
            log.info(
                    "가상 센서 실행 권한을 획득했습니다. sectionId={}, owner={}",
                    sectionId,
                    ownerToken
            );
        } catch (RuntimeException exception) {
            log.error(
                    "가상 센서 Lock 획득 또는 Flow 시작에 실패했습니다. sectionId={}",
                    sectionId,
                    exception
            );

            stopFlowQuietly(sectionId);
            releaseLockQuietly(sectionId);
        }
    }

    // 소유 중인 Lock의 TTL을 갱신하고, 실패하면 Flow를 중지한다.
    private void renewOwnedSectionLock(Long sectionId) {
        try {
            boolean renewed = redisLeaseLockService.renew(
                    lockKey(sectionId),
                    ownerToken,
                    leaseDuration
            );

            if (renewed) {
                log.debug(
                        "가상 센서 Lock을 갱신했습니다. sectionId={}",
                        sectionId
                );
                return;
            }

            log.warn(
                    "가상 센서 Lock 소유권을 잃었습니다. sectionId={}",
                    sectionId
            );
        } catch (RuntimeException exception) {
            log.error(
                    "가상 센서 Lock 갱신에 실패했습니다. sectionId={}",
                    sectionId,
                    exception
            );
        }

        ownedSectionIds.remove(sectionId);
        stopFlowQuietly(sectionId);
    }

    // 비활성화 또는 종료 시 Flow를 먼저 중지하고 Lock을 반환한다.
    private void stopFlowAndReleaseLock(Long sectionId) {
        if (!stopFlowQuietly(sectionId)) {
            return;
        }

        releaseLockQuietly(sectionId);
    }

    private boolean stopFlowQuietly(Long sectionId) {
        try {
            flowEngine.stopAndRemoveFlow(VirtualSensorFlow.flowId(sectionId));
            return true;
        } catch (RuntimeException exception) {
            log.error(
                    "가상 센서 Flow 중지에 실패했습니다. sectionId={}",
                    sectionId,
                    exception
            );
            return false;
        }
    }

    private void releaseLockQuietly(Long sectionId) {
        try {
            boolean released = redisLeaseLockService.release(
                    lockKey(sectionId),
                    ownerToken
            );

            ownedSectionIds.remove(sectionId);

            if (released) {
                log.info(
                        "가상 센서 Lock을 해제했습니다. sectionId={}",
                        sectionId
                );
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "가상 센서 Lock 해제에 실패했습니다. TTL 만료를 기다립니다. sectionId={}",
                    sectionId,
                    exception
            );
        }
    }

    private String lockKey(Long sectionId) {
        return lockKeyPrefix + ":" + sectionId;
    }
}
