package com.nhnacademy.ruleengine.engine.location;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
// 외부 MQTT의 이름 기반 위치 정보를 룰엔진 내부 ID로 변환한다.(추후수정)
public class SectionCatalog {

    private final Map<String, Long> organizationIdsByApplicationName = new HashMap<>();
    private final Map<String, Long> storageIdsIdsByLocation = new HashMap<>();
    private final Map<String, Long> sectionIdsByPoint = new HashMap<>();

    public SectionCatalog() {

        organizationIdsByApplicationName.put("광주 캠퍼스", 1L);

        storageIdsIdsByLocation.put("사무실 밖", 1L);
        storageIdsIdsByLocation.put("사무실", 2L);
        storageIdsIdsByLocation.put("실습실", 3L);
        storageIdsIdsByLocation.put("회의실", 4L);

        sectionIdsByPoint.put("업무 공간 안쪽", 1L);
        sectionIdsByPoint.put("입구 오른쪽", 2L);
        sectionIdsByPoint.put("사무공간 스위치 옆", 3L);
        sectionIdsByPoint.put("후면 오른쪽", 4L);
        sectionIdsByPoint.put("앞문", 5L);
        sectionIdsByPoint.put("전방 우측", 6L);
        sectionIdsByPoint.put("출입문", 7L);
        sectionIdsByPoint.put("후방 오른쪽", 8L);
    }

    public Optional<ResolvedSection> resolve(
            String applicationName,
            String location,
            String point
    ) {
        if (isBlank(applicationName) || isBlank(location) || isBlank(point)) {
            return Optional.empty();
        }

        Long organizationId = organizationIdsByApplicationName.get(applicationName.trim());
        Long storageId = storageIdsIdsByLocation.get(location.trim());
        Long sectionId = sectionIdsByPoint.get(point.trim());

        if (organizationId == null || storageId == null || sectionId == null) {
            return Optional.empty();
        }

        return Optional.of(new ResolvedSection(organizationId, storageId, sectionId));
    }


    public boolean exists(Long sectionId) {
        return sectionIdsByPoint.containsValue(sectionId);
    }


    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record ResolvedSection(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
    }

    //임시(추후수정)
    public List<Long> findAllSectionIds(){
        return sectionIdsByPoint.values().stream().toList();
    }
}
