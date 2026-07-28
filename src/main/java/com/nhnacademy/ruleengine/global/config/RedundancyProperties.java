package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "rule-engine.redundancy"
)
// application.yaml의 룰엔진 이중화 설정을 타입 안전하게 관리한다.
public record RedundancyProperties(
        String instanceId,
        ExternalIngress externalIngress
) {
    public record ExternalIngress(
            String lockKey,
            Duration leaseDuration,
            Duration renewInterval
    ) {
        // 잘못된 TTL과 갱신 주기는 애플리케이션 시작 단계에서 차단한다.
        public ExternalIngress {
            if (leaseDuration != null
                    && (leaseDuration.isZero() || leaseDuration.isNegative())) {
                throw new IllegalArgumentException(
                        "lease-duration은 0보다 커야 합니다."
                );
            }

            if (renewInterval != null
                    && (renewInterval.isZero() || renewInterval.isNegative())) {
                throw new IllegalArgumentException(
                        "renew-interval은 0보다 커야 합니다."
                );
            }

            if (leaseDuration != null
                    && renewInterval != null
                    && renewInterval.compareTo(leaseDuration) >= 0) {
                throw new IllegalArgumentException(
                        "renew-interval은 lease-duration보다 짧아야 합니다."
                );
            }
        }

    }
}
