package com.nhnacademy.ruleengine.engine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
// Redis TTL 기반 Lock의 획득, 갱신, 해제를 담당한다.
public class RedisLeaseLockService {

    // 현재 소유자만 Lock의 TTL을 갱신한다.
    private static final DefaultRedisScript<Long> RENEW_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call(
                            'pexpire',
                            KEYS[1],
                            ARGV[2]
                        )
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    // 현재 소유자만 Lock Key를 삭제한다.
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('get', KEYS[1]) == ARGV[1] then
                        return redis.call('del', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class
            );

    private final StringRedisTemplate redisTemplate;

    // Key가 없을 때만 ownerToken과 TTL을 저장한다.
    public boolean acquire(String lockKey, String ownerToken, Duration leaseDuration) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(
                        lockKey,
                        ownerToken,
                        leaseDuration
                );

        return Boolean.TRUE.equals(result);
    }

    // ownerToken이 일치할 때만 TTL을 다시 설정한다.
    public boolean renew(
            String lockKey,
            String ownerToken,
            Duration leaseDuration
    ) {
        return executeOwnershipScript(
                RENEW_SCRIPT,
                lockKey,
                ownerToken,
                String.valueOf(leaseDuration.toMillis())
        );
    }

    // ownerToken이 일치할 때만 Lock을 삭제한다.
    public boolean release(String lockKey, String ownerToken) {
        return executeOwnershipScript(
                RELEASE_SCRIPT,
                lockKey,
                ownerToken
        );
    }

    // Lock을 현재 누가 소유하고 있는지 조회한다 (진단/로깅 용도).
    public String getOwner(String lockKey) {
        return redisTemplate.opsForValue().get(lockKey);
    }

    // 소유권 확인 Lua 스크립트의 공통 실행 결과를 boolean으로 변환한다.
    private boolean executeOwnershipScript(
            DefaultRedisScript<Long> script,
            String lockKey,
            Object... arguments
    ) {
        Long result = redisTemplate.execute(
                script,
                List.of(lockKey),
                arguments
        );

        return Long.valueOf(1L).equals(result);
    }
}
