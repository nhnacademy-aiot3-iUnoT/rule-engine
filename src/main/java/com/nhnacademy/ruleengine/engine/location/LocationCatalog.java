package com.nhnacademy.ruleengine.engine.location;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
// TODO 장소 관리 서비스 연동 시 applicationName + location 조회로 교체한다.
public class LocationCatalog {
    private final Map<String, Long> locationIdsByName = Map.of(
            "사무실", 1L,
            "실습실", 2L,
            "사무실 밖", 3L
    );

    public Optional<Long> resolve(String applicationName, String locationName) {
        return Optional.ofNullable(locationIdsByName.get(locationName));
    }
}
