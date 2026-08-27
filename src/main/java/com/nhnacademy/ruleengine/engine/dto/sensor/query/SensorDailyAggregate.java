package com.nhnacademy.ruleengine.engine.dto.sensor.query;

// InfluxDB에서 한 번에 집계한 센서 타입별 통계. 하루 요약의 원재료다.
// 평균은 InfluxDB가 계산한 값을 그대로 쓴다. 표본 수(count)는 이탈 비율의 분모로 쓰인다.
public record SensorDailyAggregate(
        String sensorType,
        String unit,
        long count,
        double avg,
        double min,
        double max
) {
}
