package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 하루 요약을 요약 버킷에 적재하는 배치의 설정이다.
 * <p>
 * 인스턴스 간 Lock이 없다. 적재는 같은 태그·시각의 지점을 덮어쓰는 멱등한 동작이고,
 * 이탈 비율과 임계값은 판단 시점에 Redis로 모아 둔 값을 읽으므로 어느 인스턴스가 계산해도 결과가 같다.
 * 두 인스턴스가 함께 돌아도 손해는 InfluxDB 질의가 두 배가 되는 것뿐이다.
 */
@ConfigurationProperties(prefix = "rule-engine.daily-rollup")
public record DailyRollupProperties(
        String cron
) {
    public DailyRollupProperties {
        if (cron == null || cron.isBlank()) {
            throw new IllegalArgumentException(
                    "rule-engine.daily-rollup.cron은 필수입니다."
            );
        }
    }
}
