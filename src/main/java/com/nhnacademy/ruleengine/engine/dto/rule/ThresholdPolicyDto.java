package com.nhnacademy.ruleengine.engine.dto.rule;

import java.util.Map;
import java.util.Optional;

// 환경 임계값 설정 dto. 센서타입별 범위를 Map으로 가져,
// 새 임계값 기반 센서타입이 추가돼도 이 dto 자체는 바뀌지 않는다.
//
// 임계값은 구역(zoneId)에만 종속되고 zoneId는 전역 고유하므로 조직/창고 식별자는 담지 않는다.
// zoneId로 캐싱하는 값에 상위 식별자가 섞여 있으면 누가 먼저 캐시를 채웠는지에 따라 내용이 달라진다.
public record ThresholdPolicyDto(
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
