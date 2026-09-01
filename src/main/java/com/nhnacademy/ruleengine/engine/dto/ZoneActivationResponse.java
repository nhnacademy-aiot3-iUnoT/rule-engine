package com.nhnacademy.ruleengine.engine.dto;

/**
 * 구역의 운영 여부. 저장소가 비활성이면 구역 자체가 ACTIVE여도 active는 false다.
 * zoneActive/storageActive는 어느 쪽 때문에 멈췄는지 로그로 남기기 위한 값이다.
 */
public record ZoneActivationResponse(
        Long zoneId,
        boolean active,
        boolean zoneActive,
        boolean storageActive
) {
}
