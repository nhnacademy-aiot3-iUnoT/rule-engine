package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "rule-engine.daily-rollup")
// 하루 요약을 요약 버킷에 적재하는 배치의 설정이다.
public record DailyRollupProperties(
        String cron,
        String lockKeyPrefix,

        /*
         * 적재가 끝난 뒤에도 Lock을 유지해 같은 날짜를 다시 적재하지 않게 한다.
         * 배치가 도는 시각과 다음 배치 사이보다 짧기만 하면 되므로 하루보다 짧게 잡는다.
         */
        Duration lockDuration
) {
    public DailyRollupProperties {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.daily-rollup.cron은 필수입니다."
            );
        }

        if (lockKeyPrefix == null || lockKeyPrefix.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.daily-rollup.lock-key-prefix는 필수입니다."
            );
        }

        if (lockDuration == null || lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException(
                    "rule-engine.daily-rollup.lock-duration은 0보다 커야 합니다."
            );
        }
    }
}
