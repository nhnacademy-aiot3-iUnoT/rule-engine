package com.nhnacademy.ruleengine.engine.dto.rule;

import java.util.Map;
import java.util.Optional;

// 환경 임계값 설정 dto. 센서타입별 범위를 Map으로 가져,
// 새 임계값 기반 센서타입이 추가돼도 이 dto 자체는 바뀌지 않는다.
public record ThresholdPolicyDto(
        Long organizationId,
        Long locationId,
        Long positionId,
        Map<String, ThresholdRange> ranges // key: 센서타입 (예: "temperature")
) {
    // 임계시간(alertDurationMinutes)은 인벤토리에서 센서타입별로 설정하므로 범위와 같이 둔다.
    public record ThresholdRange(Double min, Double max, Integer alertDurationMinutes) {
    }

    public Optional<ThresholdRange> rangeFor(String sensorType) {
        if (ranges == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ranges.get(sensorType));
    }
}
