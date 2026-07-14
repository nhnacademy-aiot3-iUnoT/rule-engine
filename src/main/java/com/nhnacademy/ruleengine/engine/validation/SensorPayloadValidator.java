package com.nhnacademy.ruleengine.engine.validation;

import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;

public final class SensorPayloadValidator {
    private SensorPayloadValidator() {
    }

    public static void validate(SensorPayloadDto payload) {
        requireText(payload.applicationName(), "applicationName");
        requireText(payload.deviceName(), "deviceName");
        requireText(payload.deviceEui(), "deviceEui");
        requireText(payload.location(), "location");
        requireText(payload.sensorType(), "sensorType");
        requireText(payload.unit(), "unit");
        requireText(payload.time(), "time");

        if (payload.locationId() == null || payload.locationId() <= 0) {
            throw new IllegalArgumentException("locationId는 양수여야 합니다.");
        }

        if (payload.value() == null) {
            throw new IllegalArgumentException("value는 필수입니다.");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "는 필수입니다.");
        }
    }
}
