package com.nhnacademy.ruleengine.engine.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisLeaseLockService {

    private final StringRedisTemplate redisTemplate;
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

    /**
     * lock 획득을 시도한다.
     */
    public boolean acquire(String lockKey, String ownerToken, Duration leaseDuration) {
        Boolean result = redisTemplate.opsForValue()
                .setIfAbsent(
                        lockKey,
                        ownerToken,
                        leaseDuration

                );

        return Boolean.TRUE.equals(result);
    }

    /**
     * lock을 갱신한다.
     */
    public boolean renew(
            String lockKey,
            String ownerToken,
            Duration leaseDuration
    ) {
        Long result = redisTemplate.execute(
                RENEW_SCRIPT,
                List.of(lockKey),
                ownerToken,
                String.valueOf(leaseDuration.toMillis())
        );

        return Long.valueOf(1L).equals(result); // TTL 갱신 성공시 true
    }

}
