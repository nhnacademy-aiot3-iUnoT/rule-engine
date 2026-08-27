package com.nhnacademy.ruleengine.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "influx-db")
// application 설정의 InfluxDB 접속 및 저장 정보를 담는다.
public record InfluxDbProperties(
        String url,
        String token,
        String org,
        String bucket,
        String measurement,

        // 원본 버킷은 보관 기간이 짧아 며칠만 지나도 사라진다.
        // 주간 리뷰처럼 지난 기간을 돌아보는 기능은 이 버킷에 쌓아 둔 하루 요약만 읽는다.
        String summaryBucket
) {
    public InfluxDbProperties {
        if (summaryBucket == null || summaryBucket.isBlank()) {
            throw new IllegalArgumentException(
                    "influx-db.summary-bucket은 필수입니다."
            );
        }
    }
}
