package com.nhnacademy.ruleengine.engine.dto.environment;

/**
 * 하루 동안 실제로 적용된 임계값과 그 기준으로 센 표본 수.
 */
public record SensorDailyThresholdStat(
        String sensorType,

        // 임계값 판단을 거친 측정값 수. 이탈 비율의 분모다.
        long total,

        // 그중 임계 범위를 벗어난 측정값 수.
        long outOfRange,

        // 판단에 쓰인 임계값. 한쪽만 설정된 구역이 있어 각각 null일 수 있다.
        Double min,
        Double max
) {
    public Double outOfRangeRatio() {
        if (total == 0L) {
            return null;
        }

        return Math.round((double) outOfRange / total * 1000.0) / 1000.0;
    }
}
