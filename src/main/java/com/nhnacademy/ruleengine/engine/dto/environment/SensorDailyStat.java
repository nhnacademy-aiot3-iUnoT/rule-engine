package com.nhnacademy.ruleengine.engine.dto.environment;

// 센서 타입 하나의 하루 통계.
public record SensorDailyStat(
        String sensorType,
        String unit,
        double avg,
        double min,
        double max,

        // 구역에 임계값이 설정되지 않았거나 한쪽 경계만 설정된 경우 null이다.
        Double thresholdMin,
        Double thresholdMax,

        // 임계 범위를 벗어난 측정값의 비율(0.0~1.0). 임계값이 없거나 표본이 없으면 null이다.
        Double outOfRangeRatio,

        // 전일 데이터가 없으면 null이다.
        Double previousDayAvg
) {
}
