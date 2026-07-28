package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.FlowLifecycleManager;
import com.nhnacademy.ruleengine.engine.flow.ExternalSensorFlow;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

// Redis Lock 상태에 따라 외부 MQTT 수집 Flow의 실행 여부를 조정한다.
@Slf4j
@Component
public class ExternalIngressCoordinator implements SchedulingConfigurer {

    private final RedisLeaseLockService redisLeaseLockService;
    private final FlowLifecycleManager flowLifecycleManager;
    private final ExternalSensorFlow externalSensorFlow;
    private final String ownerToken;
    private final String lockKey;
    private final Duration leaseDuration;
    private final Duration renewInterval;

    private boolean isExternalIngressLeader;
    private boolean shutdownInProgress;

    public ExternalIngressCoordinator(
            RedisLeaseLockService redisLeaseLockService,
            RedundancyProperties redundancyProperties,
            FlowLifecycleManager flowLifecycleManager,
            ExternalSensorFlow externalSensorFlow
    ) {
        this.redisLeaseLockService = redisLeaseLockService;
        this.flowLifecycleManager = flowLifecycleManager;
        this.externalSensorFlow = externalSensorFlow;

        RedundancyProperties.ExternalIngress externalIngress =
                redundancyProperties.externalIngress();

        this.ownerToken = redundancyProperties.instanceId()
                + ":" + UUID.randomUUID();
        this.lockKey = externalIngress.lockKey();
        this.leaseDuration = externalIngress.leaseDuration();
        this.renewInterval = externalIngress.renewInterval();
    }

    // 설정된 갱신 주기로 리더 상태 확인 작업을 등록한다.
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedDelayTask(
                this::checkLeadership,
                renewInterval
        );
    }

    @EventListener(ContextClosedEvent.class)
    public synchronized void handleApplicationShutdown() {
        if (shutdownInProgress) {
            return;
        }

        shutdownInProgress = true;

        if (isExternalIngressLeader) {
            stopFlowAndReleaseOwnedLock(); // Lock 및 Flow 종료
        } else {
            tryStopFlow(); // Flow 만 종료
        }
    }

    // 현재 상태에 따라 Lock 획득 또는 갱신을 수행한다.
    public synchronized void checkLeadership() {
        if (shutdownInProgress) { // 종료중엔 실행 X
            return;
        }

        // Lock 주인인경우 재발행
        if (isExternalIngressLeader) {
            renewLeadership();
            return;
        }

        // Lock 획득 시도
        tryBecomeLeader();
    }

    // Redis Lock을 획득한 뒤 외부 수집 Flow를 시작한다.
    private void tryBecomeLeader() {
        if (!tryAcquireLock()) {
            return;
        }

        try {
            flowLifecycleManager.start(
                    ExternalSensorFlow.FLOW_ID,
                    externalSensorFlow::create
            );
            isExternalIngressLeader = true;
            log.info("외부 데이터 수집 권한을 획득했습니다. owner={}", ownerToken);
        } catch (RuntimeException exception) {
            log.error(
                    "ExternalSensorFlow 시작에 실패했습니다. owner={}",
                    ownerToken,
                    exception
            );

            // Lock 해재및 Flow 종료
            stopFlowAndReleaseOwnedLock();
        }
    }

    // 대기 인스턴스가 Redis Lock 획득을 시도한다.
    private boolean tryAcquireLock() {
        try {
            boolean acquired = redisLeaseLockService.acquire(
                    lockKey,
                    ownerToken,
                    leaseDuration
            );

            if (!acquired) {
                log.debug("다른 인스턴스가 외부 수집 Lock을 소유하고 있습니다. owner={}", ownerToken);
            }

            return acquired;
        } catch (RuntimeException exception) {
            log.error(
                    "Redis Lock 획득 중 오류가 발생했습니다. owner={}",
                    ownerToken,
                    exception
            );
            return false;
        }
    }

    // 리더가 가진 Redis Lock의 TTL을 갱신한다.
    private void renewLeadership() {
        try {
            boolean renewed = redisLeaseLockService.renew(
                    lockKey,
                    ownerToken,
                    leaseDuration
            );

            if (renewed) {
                log.debug("외부 수집 Lock을 갱신했습니다. owner={}", ownerToken);
                return;
            }

            log.warn("외부 수집 Lock 소유권을 잃었습니다. owner={}", ownerToken);
        } catch (RuntimeException exception) {
            log.error(
                    "Redis Lock 갱신 중 오류가 발생했습니다. owner={}",
                    ownerToken,
                    exception
            );
        }

        isExternalIngressLeader = false;
        tryStopFlow();
    }

    // 자신이 Lock 소유자임이 확실할 때 Flow 중지 후 Lock을 반납한다.
    private void stopFlowAndReleaseOwnedLock() {
        isExternalIngressLeader = false;

        if (tryStopFlow()) {
            tryReleaseOwnedLock();
        }
    }

    // Flow 중지 시도
    private boolean tryStopFlow() {
        try {
            flowLifecycleManager.stop(
                    ExternalSensorFlow.FLOW_ID
            );
            return true;
        } catch (RuntimeException exception) {
            log.error(
                    "ExternalSensorFlow 중지에 실패했습니다. owner={}",
                    ownerToken,
                    exception
            );
            return false;
        }
    }

    // 다른 인스턴스의 Lock을 지우지 않도록 ownerToken을 확인해 해제한다.
    private void tryReleaseOwnedLock() {
        try {
            boolean released = redisLeaseLockService.release(
                    lockKey,
                    ownerToken
            );

            if (released) {
                log.info("외부 수집 Lock을 해제했습니다. owner={}", ownerToken);
            } else {
                log.debug("해제할 외부 수집 Lock이 없습니다. owner={}", ownerToken);
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "외부 수집 Lock 해제에 실패했습니다. TTL 만료를 기다립니다. owner={}",
                    ownerToken,
                    exception
            );
        }
    }
}
