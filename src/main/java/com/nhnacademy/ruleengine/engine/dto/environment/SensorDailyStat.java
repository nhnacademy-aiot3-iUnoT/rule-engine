package com.nhnacademy.ruleengine.engine.dto.environment;

// 센서 타입 하나의 하루 통계. 설명을 만들려면 측정값만으로는 부족해 판단 기준을 함께 담는다.
//
// thresholdMin/Max와 outOfRangeRatio가 있어야 "평균은 정상이었지만 하루의 12%는 상한을 넘었다"는
// 서술이 가능해지고, previousDayAvg가 있어야 "어제보다 2도 높다"는 비교가 가능해진다.
// 이 값들이 없으면 남는 설명은 "평균 22.4도입니다" 수준뿐이다.
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
