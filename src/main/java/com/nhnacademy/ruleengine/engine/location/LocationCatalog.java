package com.nhnacademy.ruleengine.engine.location;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
// 외부 MQTT의 이름 기반 위치 정보를 룰엔진 내부 ID로 변환한다.(추후수정)
public class LocationCatalog {

    private final Map<String, Long> organizationIdsByApplicationName = new HashMap<>();
    private final Map<String, Long> locationIdsByLocation = new HashMap<>();
    private final Map<String, Long> positionIdsByPoint = new HashMap<>();

    public LocationCatalog() {

        organizationIdsByApplicationName.put("광주 캠퍼스", 1L);

        locationIdsByLocation.put("사무실 밖", 1L);
        locationIdsByLocation.put("사무실", 2L);
        locationIdsByLocation.put("실습실", 3L);

        positionIdsByPoint.put("업무 공간 안쪽", 1L);
        positionIdsByPoint.put("입구 오른쪽", 2L);
        positionIdsByPoint.put("사무공간 스위치 옆", 3L);
        positionIdsByPoint.put("후면 오른쪽", 4L);
        positionIdsByPoint.put("앞문", 5L);
        positionIdsByPoint.put("전방 우측", 6L);
        positionIdsByPoint.put("출입문", 7L);
        positionIdsByPoint.put("후방 오른쪽", 5L);
    }

    public Optional<ResolvedLocation> resolve(
            String applicationName,
            String location,
            String point
    ) {
        if (isBlank(applicationName) || isBlank(location) || isBlank(point)) {
            return Optional.empty();
        }

        Long organizationId = organizationIdsByApplicationName.get(applicationName.trim());
        Long locationId = locationIdsByLocation.get(location.trim());
        Long positionId = positionIdsByPoint.get(point.trim());

        if (organizationId == null || locationId == null || positionId == null) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedLocation(organizationId, locationId, positionId));
    }


    public boolean exists(Long locationId) {
        return locationIdsByLocation.containsValue(locationId);
    }



    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ResolvedLocation(
            Long organizationId,
            Long locationId,
            Long positionId
    ) {
    }
}
