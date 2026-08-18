package com.nhnacademy.ruleengine.engine.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramApiResponse(
        boolean ok,
        @JsonProperty("error_code")
        Integer errorCode,
        String description

) {
}
