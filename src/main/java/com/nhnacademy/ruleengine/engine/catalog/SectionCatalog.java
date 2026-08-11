package com.nhnacademy.ruleengine.engine.catalog;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

//임시 클래스 추후 삭제예정
@Component
public class SectionCatalog {

    private final Map<String, ResolvedSection> sectionMap = new HashMap<>();


    public SectionCatalog() {
        sectionMap.put("24e124128c067999", new ResolvedSection(1L, 1L, 1L));
        sectionMap.put("24e124141d180806", new ResolvedSection(1L, 1L, 1L));

        sectionMap.put("24e124128c140101", new ResolvedSection(1L, 1L, 2L));
        sectionMap.put("24e124136d151836", new ResolvedSection(1L, 2L, 4L));

        sectionMap.put("24e124785c389010", new ResolvedSection(1L, 1L, 3L));

        sectionMap.put("24e124141d189196", new ResolvedSection(1L, 1L, 2L));
        sectionMap.put("24e124126d152862", new ResolvedSection(1L, 2L, 5L));

        sectionMap.put("24e124126d152590", new ResolvedSection(1L, 3L, 6L));
        sectionMap.put("24e124136d151547", new ResolvedSection(1L, 3L, 7L));
        sectionMap.put("24e124136d151606", new ResolvedSection(1L, 3L, 8L));

        sectionMap.put("24e124725d081175", new ResolvedSection(1L, 4L, 9L));
        sectionMap.put("24e124725d089152", new ResolvedSection(1L, 4L, 10L));

        sectionMap.put("24e124785c389818", new ResolvedSection(2L, 5L, 11L));
        sectionMap.put("24e124743d012436", new ResolvedSection(2L, 5L, 12L));


    }

    public Optional<ResolvedSection> resolveSection(
            String deviceEui
    ) {
        if (deviceEui == null || deviceEui.isEmpty()) {
            return Optional.empty();
        }

        return Optional.ofNullable(sectionMap.get(deviceEui));

    }

    public record ResolvedSection(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
    }
}