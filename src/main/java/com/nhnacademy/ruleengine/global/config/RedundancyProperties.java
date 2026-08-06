package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(
        prefix = "rule-engine.redundancy"
)
// application.yaml의 룰엔진 이중화 설정을 타입 안전하게 관리한다.
public record RedundancyProperties(
        String instanceId,
        LeaseSettings externalIngress,
        LeaseSettings virtualSensor
) {
    public RedundancyProperties {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.redundancy.instance-id는 필수입니다."
            );
        }

        if (externalIngress == null) {
            throw new IllegalArgumentException(
                    "rule-engine.redundancy.external-ingress 설정이 필요합니다."
            );
        }

        if (virtualSensor == null) {
            throw new IllegalArgumentException(
                    "rule-engine.redundancy.virtual-sensor 설정이 필요합니다."
            );
        }
    }

    public record LeaseSettings(
            String lockKey,
            Duration leaseDuration,
            Duration renewInterval
    ) {
        public LeaseSettings {
            if (lockKey == null || lockKey.isBlank()) {
                throw new IllegalArgumentException(
                        "lock-key는 필수입니다."
                );
            }

            requirePositive(leaseDuration, "lease-duration");
            requirePositive(renewInterval, "renew-interval");

            if (renewInterval.compareTo(leaseDuration) >= 0) {
                throw new IllegalArgumentException(
                        "renew-interval은 lease-duration보다 짧아야 합니다. lockKey=" + lockKey
                );
            }
        }

        private static void requirePositive(Duration value, String propertyName) {
            if (value == null || value.isZero() || value.isNegative()) {
                throw new IllegalArgumentException(
                        propertyName + "은 0보다 커야 합니다."
                );
            }
        }
    }
}
