package com.nhnacademy.ruleengine.engine.validation;

import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;

public final class SensorPayloadValidator {
    private SensorPayloadValidator() {
    }

    public static void validate(SensorPayloadDto payload) {
        requirePositive(payload.organizationId(), "organizationId");
        requireText(payload.deviceEui(), "deviceEui");
        requirePositive(payload.storageId(), "storageId");
        requirePositive(payload.sectionId(), "sectionId");
        requireText(payload.sensorType(), "sensorType");
        requireText(payload.unit(), "unit");
        requireText(payload.time(), "time");

        if (payload.value() == null) {
            throw new IllegalArgumentException("value는 필수입니다.");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }
}
