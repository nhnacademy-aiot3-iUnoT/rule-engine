package com.nhnacademy.ruleengine.engine.dto.sensor;


import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog.ResolvedSection;

// 센서별 변환 로직에서 공통으로 사용하는 장치 정보를 담는다.
public record SensorContext(
        Long organizationId,
        String deviceEui,
        Long storageId,
        Long sectionId,
        String time
) {

    public static SensorContext from(
            ExternalSensorMessage externalSensorMessage,
            ResolvedSection resolvedSection
    ) {
        if (resolvedSection == null) {
            throw new IllegalArgumentException("변환된 섹션 ID는 필수입니다.");
        }

        return new SensorContext(
                resolvedSection.organizationId(),
                valueOrUnknown(externalSensorMessage.devEui()),
                resolvedSection.storageId(),
                resolvedSection.sectionId(),
                valueOrUnknown(externalSensorMessage.time())
        );
    }

    private static String valueOrUnknown(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value;
    }
}
