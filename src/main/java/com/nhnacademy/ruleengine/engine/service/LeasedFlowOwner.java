package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.global.config.RedundancyProperties.LeaseSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
public abstract class LeasedFlowOwner<K> implements SchedulingConfigurer {

    private final RedisLeaseLockService lockService;
    private final FlowEngine flowEngine;
    private final Clock clock;

    private final String ownerToken;
    private final Duration leaseDuration;
    private final Duration renewInterval;

    /**
     * 마지막 갱신 성공 이후 이 시간이 지나면 소유권을 잃었다고 보고 스스로 Flow를 중지한다.
     */
    private final Duration fenceAfter;

    /**
     * 소유 중인 키 -> 마지막으로 Lock 갱신을 시도한 시각(성공한 것만 기록)
     */
    private final Map<K, Instant> leaseRenewedAt = new HashMap<>();

    private boolean shutdownInProgress;

    protected LeasedFlowOwner(
            RedisLeaseLockService lockService,
            FlowEngine flowEngine,
            String instanceId,
            LeaseSettings leaseSettings
    ) {
        this(lockService, flowEngine, instanceId, leaseSettings, Clock.systemUTC());
    }

    // 자기 차단 시점은 시간에 의존하므로 테스트에서 Clock을 주입할 수 있게 열어둔다.
    protected LeasedFlowOwner(
            RedisLeaseLockService lockService,
            FlowEngine flowEngine,
            String instanceId,
            LeaseSettings leaseSettings,
            Clock clock
    ) {
        this.lockService = lockService;
        this.flowEngine = flowEngine;
        this.clock = clock;
        this.ownerToken = instanceId + ":" + UUID.randomUUID();
        this.leaseDuration = leaseSettings.leaseDuration();
        this.renewInterval = leaseSettings.renewInterval();

        // 확인 주기만큼의 여유를 둬서, 늦어도 Redis에서 TTL이 만료되기 전에 스스로 중지하도록 한다.
        this.fenceAfter = leaseDuration.minus(renewInterval);
    }

    // 이번 주기에 소유해야 하는 키 목록. 조회에 실패하면 예외를 던지면 된다.
    protected abstract Set<K> desiredKeys();

    protected abstract String lockKey(K key);

    protected abstract String flowId(K key);

    // Lock을 획득한 뒤 실행할 Flow를 새로 만든다. 호출될 때마다 새 인스턴스를 반환해야 한다.
    protected abstract Flow createFlow(K key);

    /**
     * 소유권을 유지한 키마다 갱신 성공 직후 호출된다.
     * 설정 변경 반영처럼 주기적으로 확인해야 할 일이 있는 하위 클래스가 재정의한다.
     */
    protected void onLeaseRenewed(K key) {
        // 기본 동작 없음
    }

    // 소유권을 내려놓은 뒤 호출된다. 키에 묶어둔 로컬 상태가 있는 하위 클래스가 재정의한다.
    protected void onOwnershipReleased(K key) {
        // 기본 동작 없음
    }

    // 소유 중인 Flow를 최신 상태로 다시 시작한다. onLeaseRenewed()에서 사용한다.
    protected final void restartFlow(K key) {
        stopFlowQuietly(key);
        flowEngine.ensureStarted(createFlow(key));
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addFixedDelayTask(this::reconcile, renewInterval);
    }

    // 목표 상태와 현재 소유 상태를 맞춘다.
    public final synchronized void reconcile() {
        if (shutdownInProgress) {
            return;
        }

        fenceExpiredLeases();

        Set<K> desiredKeys;
        try {
            desiredKeys = desiredKeys();
        } catch (RuntimeException exception) {
            log.error("[{}] 목표 상태 조회에 실패해 이번 주기를 건너뜁니다.", name(), exception);
            return;
        }

        releaseUndesired(desiredKeys);
        renewOwned(desiredKeys);
        acquireMissing(desiredKeys);
    }

    @EventListener(ContextClosedEvent.class)
    public final synchronized void handleApplicationShutdown() {
        if (shutdownInProgress) {
            return;
        }

        shutdownInProgress = true;

        for (K key : Set.copyOf(leaseRenewedAt.keySet())) {
            releaseOwnership(key);
        }
    }

    /**
     * 소유권을 내려놓는다.
     * <p>
     * Flow 중지가 실패하더라도 로컬 소유 정보는 반드시 제거한다.
     * 남겨두면 매 주기 같은 실패를 반복하면서 갱신도 되지 않는 상태로 고착되기 때문이다.
     * Lock 해제는 ownerToken을 확인하므로 이미 다른 인스턴스가 가져간 Lock을 건드리지 않는다.
     */
    private void releaseOwnership(K key) {
        leaseRenewedAt.remove(key);
        stopFlowQuietly(key);
        releaseLockQuietly(key);

        try {
            onOwnershipReleased(key);
        } catch (RuntimeException exception) {
            log.error("[{}] key={} 소유권 해제 후 처리에 실패했습니다.", name(), key, exception);
        }
    }

    /**
     * 갱신이 늦어져 Lock이 만료됐을 수 있는 키의 Flow를 중지한다.
     * <p>
     * Redis가 응답하지 않아 renew를 아예 시도하지 못한 경우에도 반드시 동작해야 하므로,
     * Redis 호출 결과가 아니라 로컬 시각만 보고 판단한다.
     */
    private void fenceExpiredLeases() {
        Instant now = clock.instant();

        for (Map.Entry<K, Instant> entry : Map.copyOf(leaseRenewedAt).entrySet()) {
            K key = entry.getKey();

            if (entry.getValue().plus(fenceAfter).isAfter(now)) {
                continue;
            }

            log.warn(
                    "[{}] key={} Lock을 제때 갱신하지 못해 Flow를 중지합니다. lastRenewedAt={}",
                    name(),
                    key,
                    entry.getValue()
            );

            releaseOwnership(key);
        }
    }

    // 더 이상 실행 대상이 아닌 키의 Flow를 중지하고 Lock을 반납한다.
    private void releaseUndesired(Set<K> desiredKeys) {
        for (K key : Set.copyOf(leaseRenewedAt.keySet())) {
            if (desiredKeys.contains(key)) {
                continue;
            }

            log.info("[{}] key={} 실행 대상에서 제외되어 Flow를 중지합니다.", name(), key);

            releaseOwnership(key);
        }
    }

    private void renewOwned(Set<K> desiredKeys) {
        for (K key : Set.copyOf(leaseRenewedAt.keySet())) {
            if (!desiredKeys.contains(key) || !renewLease(key)) {
                continue;
            }

            try {
                onLeaseRenewed(key);
            } catch (RuntimeException exception) {
                // Flow 재시작 도중 실패했다면 이 인스턴스는 Lock만 쥔 채 아무것도 하지 않는 상태일 수 있다.
                // 소유권을 반납해 다음 주기에 처음부터 다시 시작하게 한다.
                log.error("[{}] key={} 갱신 후 처리에 실패해 소유권을 반납합니다.", name(), key, exception);
                releaseOwnership(key);
            }
        }
    }

    /**
     * Lock TTL을 갱신한다.
     * <p>
     * 갱신 실패(소유권 상실)는 즉시 Flow를 중지하지만, Redis 오류는 그렇지 않다.
     * 일시적인 장애로 매번 Flow를 껐다 켜면 오히려 데이터가 끊기므로,
     * 오류일 때는 소유 상태를 유지하고 {@link #fenceExpiredLeases()}가 TTL 기준으로 판단하게 둔다.
     */
    private boolean renewLease(K key) {
        // Redis에 TTL이 실제로 설정되는 시각보다 앞선 값을 기준으로 삼아야 안전한 방향으로 오차가 생긴다.
        Instant attemptedAt = clock.instant();

        try {
            if (lockService.renew(lockKey(key), ownerToken, leaseDuration)) {
                leaseRenewedAt.put(key, attemptedAt);
                log.debug("[{}] key={} Lock을 갱신했습니다.", name(), key);
                return true;
            }

            log.warn("[{}] key={} Lock 소유권을 잃어 Flow를 중지합니다.", name(), key);

            releaseOwnership(key);
            return false;
        } catch (RuntimeException exception) {
            log.error(
                    "[{}] key={} Lock 갱신 중 오류가 발생했습니다. TTL이 만료되면 Flow를 중지합니다.",
                    name(),
                    key,
                    exception
            );
            return false;
        }
    }

    private void acquireMissing(Set<K> desiredKeys) {
        for (K key : desiredKeys) {
            if (leaseRenewedAt.containsKey(key)) {
                continue;
            }

            acquireAndStart(key);
        }
    }

    private void acquireAndStart(K key) {
        Instant attemptedAt = clock.instant();

        try {
            if (!lockService.acquire(lockKey(key), ownerToken, leaseDuration)) {
                log.debug(
                        "[{}] key={} 다른 인스턴스가 Lock을 소유하고 있습니다. currentOwner={}",
                        name(),
                        key,
                        lockService.getOwner(lockKey(key))
                );
                return;
            }
        } catch (RuntimeException exception) {
            log.error("[{}] key={} Lock 획득 중 오류가 발생했습니다.", name(), key, exception);
            return;
        }

        // Lock을 잡은 다음부터는 실패하더라도 반드시 반납해야 다른 인스턴스가 이어받을 수 있다.
        try {
            flowEngine.ensureStarted(createFlow(key));
            leaseRenewedAt.put(key, attemptedAt);
            log.info("[{}] key={} 실행 권한을 획득했습니다. owner={}", name(), key, ownerToken);
        } catch (RuntimeException exception) {
            log.error("[{}] key={} Flow 시작에 실패했습니다.", name(), key, exception);

            releaseOwnership(key);
        }
    }

    private void stopFlowQuietly(K key) {
        try {
            flowEngine.stopAndRemoveFlow(flowId(key));
        } catch (RuntimeException exception) {
            log.error("[{}] key={} Flow 중지에 실패했습니다.", name(), key, exception);
        }
    }

    private void releaseLockQuietly(K key) {
        try {
            if (lockService.release(lockKey(key), ownerToken)) {
                log.info("[{}] key={} Lock을 해제했습니다.", name(), key);
            }
        } catch (RuntimeException exception) {
            log.warn(
                    "[{}] key={} Lock 해제에 실패했습니다. TTL 만료를 기다립니다.",
                    name(),
                    key,
                    exception
            );
        }
    }

    private String name() {
        return getClass().getSimpleName();
    }
}
