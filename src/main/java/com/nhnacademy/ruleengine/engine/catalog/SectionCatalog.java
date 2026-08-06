package com.nhnacademy.ruleengine.engine.catalog;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

//임시 클래스 추후 삭제예정
@Component
public class SectionCatalog {

    private final Map<String, Long> organizationIdsByApplicationName = new HashMap<>();
    private final Map<String, Long> storageIdsByLocation = new HashMap<>();
    private final Map<String, Long> sectionIdsByPoint = new HashMap<>();

    public SectionCatalog() {
        organizationIdsByApplicationName.put("광주 캠퍼스", 1L);

        storageIdsByLocation.put("사무실 밖", 1L);
        storageIdsByLocation.put("사무실", 2L);
        storageIdsByLocation.put("실습실", 3L);
        storageIdsByLocation.put("회의실", 4L);

        sectionIdsByPoint.put("업무 공간 안쪽", 1L);
        sectionIdsByPoint.put("입구 오른쪽", 2L);
        sectionIdsByPoint.put("사무공간 스위치 옆", 3L);
        sectionIdsByPoint.put("후면 오른쪽", 4L);
        sectionIdsByPoint.put("앞문", 5L);
        sectionIdsByPoint.put("전방 우측", 6L);
        sectionIdsByPoint.put("출입문", 7L);
        sectionIdsByPoint.put("후방 오른쪽", 8L);
        sectionIdsByPoint.put("구역", 9L);
    }

    public Optional<ResolvedSection> resolveSection(
            String applicationName,
            String location,
            String point
    ) {
        if (applicationName == null || applicationName.isBlank()
                || location == null || location.isBlank()) {
            return Optional.empty();
        }

        if (point == null || point.isBlank()) {
            point = "구역";
        }

        Long organizationId = organizationIdsByApplicationName.get(applicationName.trim());
        Long storageId = storageIdsByLocation.get(location.trim());

        if (organizationId == null || storageId == null) {
            return Optional.empty();
        }

        Long sectionId = sectionIdsByPoint.getOrDefault(
                point.trim(),
                sectionIdsByPoint.get("구역")
        );

        return Optional.of(new ResolvedSection(
                organizationId,
                storageId,
                sectionId
        ));
    }

    public record ResolvedSection(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
    }
}