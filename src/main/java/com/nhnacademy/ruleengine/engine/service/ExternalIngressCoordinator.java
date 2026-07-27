package com.nhnacademy.ruleengine.engine.service;

import java.time.Duration;
import java.util.UUID;

import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.engine.flow.ExternalSensorFlow;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ExternalIngressCoordinator implements SchedulingConfigurer {

    private final RedisLeaseLockService redisLeaseLockService;
    private final FlowEngine flowEngine;
    private final ExternalSensorFlow externalSensorFlow;
    private final String ownerToken;
    private final String lockKey;
    private final Duration leaseDuration;
    private final Duration renewInterval;
    private boolean leader;


    public ExternalIngressCoordinator(
            RedisLeaseLockService redisLeaseLockService,
            RedundancyProperties redundancyProperties,
            FlowEngine flowEngine,
            ExternalSensorFlow externalSensorFlow
    ) {
        this.redisLeaseLockService = redisLeaseLockService;
        this.flowEngine = flowEngine;
        this.externalSensorFlow = externalSensorFlow;

        RedundancyProperties.ExternalIngress externalIngress =
                redundancyProperties.externalIngress();

        this.ownerToken = redundancyProperties.instanceId()
                + ":" + UUID.randomUUID();
        this.lockKey = externalIngress.lockKey();
        this.leaseDuration = externalIngress.leaseDuration();
        this.renewInterval = externalIngress.renewInterval();
    }

    public void tryAcquireLock() {
        boolean acquiredLock = redisLeaseLockService.acquire(
                lockKey,
                ownerToken,
                leaseDuration
        );

        if (acquiredLock) {
            log.info("Successfully acquired Redundancy lock for {}", ownerToken);
            leader = true;

            startExternalIngress();
        } else {
            log.info("Failed to acquire Redundancy lock for {}", ownerToken);
            leader = false;
        }
    }

    public synchronized void maintainLock() {
        if (!leader) {
            tryAcquireLock();
            return;
        }

        boolean renewed = redisLeaseLockService.renew(
                lockKey,
                ownerToken,
                leaseDuration
        );

        if (renewed) {
            log.debug(
                    "Renewed Redundancy lock for {}",
                    ownerToken
            );
            return;
        }

        leader = false;

        log.warn(
                "Lost Redundancy lock for {}",
                ownerToken
        );

        stopExternalIngress();
    }

    private void startExternalIngress() {
        if (flowEngine.getFlows().containsKey(
                ExternalSensorFlow.FLOW_ID
        )) {
            flowEngine.startFlow(
                    ExternalSensorFlow.FLOW_ID
            );
        } else {
            flowEngine.registerAndStart(
                    externalSensorFlow.create()
            );
        }

        log.info(
                "ExternalSensorFlow started by {}",
                ownerToken
        );
    }

    private void stopExternalIngress() {
        if (!flowEngine.getFlows().containsKey(
                ExternalSensorFlow.FLOW_ID
        )) {
            return;
        }

        flowEngine.stopFlow(
                ExternalSensorFlow.FLOW_ID
        );

        log.info(
                "ExternalSensorFlow stopped by {}",
                ownerToken
        );
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedDelayTask(
                this::maintainLock,
                renewInterval
        );
    }


}
