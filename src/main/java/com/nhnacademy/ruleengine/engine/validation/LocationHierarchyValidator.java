package com.nhnacademy.ruleengine.engine.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocationHierarchyValidator {

    // TODO 장소관리 서비스 API Client 주입

    public void validateOrganization(Long organizationId) {
        validatePositiveId(
                organizationId,
                "organizationId"
        );

        // TODO 장소관리 서비스에서 조직 존재 여부 조회
    }

    public void validateStorage(
            Long organizationId,
            Long storageId
    ) {
        validatePositiveId(
                organizationId,
                "organizationId"
        );

        validatePositiveId(
                storageId,
                "storageId"
        );

        // TODO storageId가 organizationId 소속인지 조회
    }

    public void validateSection(
            Long organizationId,
            Long storageId,
            Long sectionId
    ) {
        validatePositiveId(
                organizationId,
                "organizationId"
        );

        validatePositiveId(
                storageId,
                "storageId"
        );

        validatePositiveId(
                sectionId,
                "sectionId"
        );

        // TODO sectionId가 organizationId와 storageId 소속인지 조회
    }

    private void validatePositiveId(
            Long id,
            String fieldName
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    fieldName + "는 1 이상의 값이어야 합니다."
            );
        }
    }
}