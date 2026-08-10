package com.nhnacademy.ruleengine.engine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramApiResponse(
        boolean ok,
        @JsonProperty("error_code")
        Integer errorCode,
        String description

) {
}
