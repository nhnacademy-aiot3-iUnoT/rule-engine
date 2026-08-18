package com.nhnacademy.ruleengine.engine.dto.inventory;

import java.math.BigDecimal;

// 인벤토리 GET /api/core/internal/zones/{zone-id}/zone-threshold 응답 항목.
// sensorTypeName은 인벤토리 sensor_types.name(예: "TEMPERATURE")이다.
public record ThresholdSpecResponse(
        Long zoneId,
        Long sensorTypeId,
        String sensorTypeName,
        BigDecimal minValue,
        BigDecimal maxValue,
        Integer alertDuration
) {
}
