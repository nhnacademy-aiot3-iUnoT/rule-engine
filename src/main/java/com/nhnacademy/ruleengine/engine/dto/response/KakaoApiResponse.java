package com.nhnacademy.ruleengine.engine.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoApiResponse(
        @JsonProperty("result_code")
        Integer resultCode
) {
}
