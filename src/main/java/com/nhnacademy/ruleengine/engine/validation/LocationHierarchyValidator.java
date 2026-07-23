package com.nhnacademy.ruleengine.engine.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationHierarchyValidator {

    // TODO 추후 DB통해 조회로 변경

    public void validateOrganization(Long organizationId) {
        // 검증
    }

    public void validateStorage(
            Long organizationId,
            Long storageId
    ) {
        validateOrganization(organizationId);
        // 저장소 소속 검증
    }

    public void validateSection(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        validateStorage(organizationId, storageId);
        // Section 소속 검증
    }
}