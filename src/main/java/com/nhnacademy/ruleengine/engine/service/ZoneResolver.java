package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneResolver {

    private final CachedZoneLookup cachedZoneLookup;

    public Optional<ResolvedZoneResponse> resolve(
            String deviceEui
    ) {
        try {
            return cachedZoneLookup.find(deviceEui);

        } catch (ApiException e) {
            log.warn(
                    "구역 정보 조회에 실패했습니다. deviceEui={}, errorCode={}, reason={}",
                    deviceEui,
                    e.getErrorCode(),
                    e.getMessage()
            );

            return Optional.empty();
        }
    }
}
