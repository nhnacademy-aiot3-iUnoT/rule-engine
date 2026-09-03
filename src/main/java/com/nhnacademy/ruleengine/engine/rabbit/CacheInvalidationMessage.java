package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CacheInvalidationMessage(
        String cacheName,
        String key
) {
}
