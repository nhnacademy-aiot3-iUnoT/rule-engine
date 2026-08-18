package com.nhnacademy.ruleengine.engine.dto.notification;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoMessageTemplate(
        @JsonProperty("object_type")
        String objectType,
        String text,
        KakaoLink link,
        @JsonProperty("button_title")
        String buttonTitle
) {
    public record KakaoLink(
            @JsonProperty("web_url")
            String webUrl,
            @JsonProperty("mobile_web_url")
            String mobileWebUrl
    ){
    }

}
