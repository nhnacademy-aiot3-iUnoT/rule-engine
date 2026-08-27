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

/**
 * JWT에서 뽑은 계정 UUID로 인벤토리에 소속 조직을 물어보고, 요청한 조직/역할과 맞는지 검증한다.
 * 인벤토리 호출이 실패하면 예외가 그대로 올라가 요청이 거부된다(fail-closed).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationAccessService {

    private final CachedMemberOrganizationLookup memberOrganizationLookup;
    private final CachedZoneLookup zoneLookup;

    /**
     * 계정의 소속 조직과 역할을 조회한다. 소속 조직이 없으면 거부한다.
     */
    public MemberOrganizationResponse getMembership(UUID accountUuid) {
        return memberOrganizationLookup.find(accountUuid)
                .orElseThrow(() -> {
                    log.warn("소속 조직이 없어 요청을 거부합니다. accountUuid={}", accountUuid);
                    return new OrganizationAccessDeniedException(ErrorCode.MEMBER_ORG_NOT_FOUND);
                });
    }

    /**
     * 요청한 조직이 계정의 소속 조직과 같은지 검증한다.
     */
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

    /**
     * 조직 보스/오너만 허용한다. 생성·수정·삭제처럼 상태를 바꾸는 요청에 쓴다.
     * 일반 조직원(ORG_MEMBER)은 조회만 할 수 있다.
     */
    public MemberOrganizationResponse verifyOwnerOrBoss(UUID accountUuid, Long organizationId) {
        return verifyRole(
                accountUuid,
                organizationId,
                OrganizationRole.ORG_BOSS,
                OrganizationRole.ORG_OWNER
        );
    }

    /**
     * 경로의 구역이 요청한 조직의 것인지까지 검증한다.
     * 조직만 맞추면 남의 조직 구역 번호로 조회가 뚫리므로, 구역을 다루는 요청에 쓴다.
     */
    public MemberOrganizationResponse verifyZone(UUID accountUuid, Long organizationId, Long zoneId) {
        MemberOrganizationResponse membership = verifyOrganization(accountUuid, organizationId);

        verifyZoneBelongsTo(accountUuid, organizationId, zoneId);

        return membership;
    }

    /**
     * 구역 검증에 더해 조직 보스/오너만 허용한다. 구역의 상태를 바꾸는 요청에 쓴다.
     */
    public MemberOrganizationResponse verifyZoneOwnerOrBoss(UUID accountUuid, Long organizationId, Long zoneId) {
        MemberOrganizationResponse membership = verifyOwnerOrBoss(accountUuid, organizationId);

        verifyZoneBelongsTo(accountUuid, organizationId, zoneId);

        return membership;
    }

    /**
     * 소속 조직 검증에 더해, 허용된 조직 역할인지까지 검증한다.
     */
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

    // 구역이 어느 조직 것인지는 인벤토리만 아는 정보라, 캐시된 내부 API 조회로 확인한다.
    // 없는 구역도 조직 자원 여부를 알 수 없으므로 거부한다(fail-closed).
    private void verifyZoneBelongsTo(UUID accountUuid, Long organizationId, Long zoneId) {
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
    }
}
