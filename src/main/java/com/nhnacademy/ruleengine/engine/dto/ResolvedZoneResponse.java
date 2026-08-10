package com.nhnacademy.ruleengine.engine.dto;

public record ResolvedZoneResponse(
        Long organizationId,
        Long storageId,
        Long zoneId
) {
}