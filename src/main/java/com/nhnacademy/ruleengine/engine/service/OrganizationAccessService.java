package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.MemberOrganizationResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.OrganizationRole;
import com.nhnacademy.ruleengine.engine.exception.OrganizationAccessDeniedException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final CachedMemberOrganizationLookup memberOrganizationLookup;
    private final CachedZoneLookup zoneLookup;


    // 로그인된 계정의 조직과 역활을 조회
    public MemberOrganizationResponse getMembership(UUID accountUuid) {
        return memberOrganizationLookup.find(accountUuid)
                .orElseThrow(() -> {
                    log.warn("소속 조직이 없어 요청을 거부합니다. accountUuid={}", accountUuid);
                    return new OrganizationAccessDeniedException(ErrorCode.MEMBER_ORG_NOT_FOUND);
                });
    }

    // 요청한 조직이 계정의 소속 조직과 같은지 검증
    public MemberOrganizationResponse verifyOrganization(UUID accountUuid, Long organizationId) {
        MemberOrganizationResponse membership = getMembership(accountUuid);

        if (!membership.belongsTo(organizationId)) {
            log.warn(
                    "다른 조직의 자원에 접근을 시도했습니다. accountUuid={}, 요청 organizationId={}, 소속 organizationId={}",
                    accountUuid,
                    organizationId,
                    membership.organizationId()
            );

            throw new OrganizationAccessDeniedException(ErrorCode.ORGANIZATION_ACCESS_DENIED);
        }

        return membership;
    }

   // 조직의 오너 혹은 보스인지 검증
    public MemberOrganizationResponse verifyOwnerOrBoss(UUID accountUuid, Long organizationId) {
        return verifyRole(
                accountUuid,
                organizationId,
                OrganizationRole.ORG_BOSS,
                OrganizationRole.ORG_OWNER
        );
    }

    // 해당구역이 해당 사용자의 조직의 구역인지 검증
    public MemberOrganizationResponse verifyZone(UUID accountUuid, Long organizationId, Long zoneId) {
        MemberOrganizationResponse membership = verifyOrganization(accountUuid, organizationId);

        resolveZoneInOrganization(accountUuid, organizationId, zoneId);

        return membership;
    }


    // 해당 조직의 보스,오너인지 검증 및 해당 구역이 조직의 구역인지 검증
    public MemberOrganizationResponse verifyZoneOwnerOrBoss(UUID accountUuid, Long organizationId, Long zoneId) {
        MemberOrganizationResponse membership = verifyOwnerOrBoss(accountUuid, organizationId);

        resolveZoneInOrganization(accountUuid, organizationId, zoneId);

        return membership;
    }

    // 해당 구역 검증 + 경로의 저장소가 그 구역이 실제로 속한 저장소인지 검증
    // 저장소 번호가 센서 데이터 태그로 저장되므로, 틀리면 남의 저장소 밑에 데이터가 쌓인다
    public MemberOrganizationResponse verifyZoneInStorage(
            UUID accountUuid,
            Long organizationId,
            Long storageId,
            Long zoneId
    ) {
        MemberOrganizationResponse membership = verifyOrganization(accountUuid, organizationId);

        verifyStorageOf(accountUuid, storageId, resolveZoneInOrganization(accountUuid, organizationId, zoneId));

        return membership;
    }

    // 해당 조직의 보스,오너인지 검증 및 해당 구역·저장소가 조직의 것인지 검증
    public MemberOrganizationResponse verifyZoneInStorageOwnerOrBoss(
            UUID accountUuid,
            Long organizationId,
            Long storageId,
            Long zoneId
    ) {
        MemberOrganizationResponse membership = verifyOwnerOrBoss(accountUuid, organizationId);

        verifyStorageOf(accountUuid, storageId, resolveZoneInOrganization(accountUuid, organizationId, zoneId));

        return membership;
    }

    // 조직 및 역활 검증
    public MemberOrganizationResponse verifyRole(
            UUID accountUuid,
            Long organizationId,
            OrganizationRole... allowedRoles
    ) {
        MemberOrganizationResponse membership = verifyOrganization(accountUuid, organizationId);

        if (!membership.hasAnyRole(allowedRoles)) {
            log.warn(
                    "권한이 없는 역할의 요청입니다. accountUuid={}, organizationId={}, role={}",
                    accountUuid,
                    organizationId,
                    membership.organizationRole()
            );

            throw new OrganizationAccessDeniedException(ErrorCode.ORGANIZATION_ROLE_FORBIDDEN);
        }

        return membership;
    }

    private ResolvedZoneResponse resolveZoneInOrganization(UUID accountUuid, Long organizationId, Long zoneId) {
        ResolvedZoneResponse location = zoneLookup.findLocation(zoneId)
                .orElseThrow(() -> {
                    log.warn(
                            "존재하지 않는 구역이라 요청을 거부합니다. accountUuid={}, organizationId={}, zoneId={}",
                            accountUuid,
                            organizationId,
                            zoneId
                    );

                    return new OrganizationAccessDeniedException(ErrorCode.ZONE_ACCESS_DENIED);
                });

        if (!Objects.equals(location.organizationId(), organizationId)) {
            log.warn(
                    "다른 조직의 구역에 접근을 시도했습니다. accountUuid={}, 요청 organizationId={}, zoneId={}, 구역 organizationId={}",
                    accountUuid,
                    organizationId,
                    zoneId,
                    location.organizationId()
            );

            throw new OrganizationAccessDeniedException(ErrorCode.ZONE_ACCESS_DENIED);
        }

        return location;
    }

    // 해당 저장소가 구역에 존재하는지 검증
    private void verifyStorageOf(UUID accountUuid, Long storageId, ResolvedZoneResponse location) {
        if (!Objects.equals(location.storageId(), storageId)) {
            log.warn(
                    "구역이 속하지 않은 저장소로 요청했습니다. accountUuid={}, 요청 storageId={}, zoneId={}, 구역 storageId={}",
                    accountUuid,
                    storageId,
                    location.zoneId(),
                    location.storageId()
            );

            throw new OrganizationAccessDeniedException(ErrorCode.STORAGE_ACCESS_DENIED);
        }
    }
}
