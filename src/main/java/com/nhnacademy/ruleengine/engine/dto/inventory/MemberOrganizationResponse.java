package com.nhnacademy.ruleengine.engine.dto.inventory;

import java.util.UUID;

// 인벤토리 내부 API(/api/core/internal/members/{account-uuid}/organization)의 응답
public record MemberOrganizationResponse(
        UUID accountUuid,
        Long organizationId,
        OrganizationRole organizationRole
) {
    public boolean belongsTo(Long organizationId) {
        return this.organizationId != null && this.organizationId.equals(organizationId);
    }

    public boolean hasAnyRole(OrganizationRole... roles) {
        for (OrganizationRole role : roles) {
            if (this.organizationRole == role) {
                return true;
            }
        }

        return false;
    }
}
