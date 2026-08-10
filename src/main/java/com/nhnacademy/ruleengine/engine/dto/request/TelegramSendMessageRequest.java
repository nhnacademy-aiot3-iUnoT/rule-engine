package com.nhnacademy.ruleengine.engine.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TelegramSendMessageRequest(
        @JsonProperty("chat_id")
        String chatId,
        String text
//        @JsonProperty("parse_mode")
//        String parseMode
) {
}
