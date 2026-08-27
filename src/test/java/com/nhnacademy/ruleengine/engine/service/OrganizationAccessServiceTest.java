package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.MemberOrganizationResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.OrganizationRole;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.engine.exception.OrganizationAccessDeniedException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationAccessServiceTest {

    private static final UUID ACCOUNT_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static final Long ORGANIZATION_ID = 7L;

    private static final Long STORAGE_ID = 3L;

    private static final Long ZONE_ID = 11L;

    @Mock
    private CachedMemberOrganizationLookup memberOrganizationLookup;

    @Mock
    private CachedZoneLookup zoneLookup;

    @InjectMocks
    private OrganizationAccessService organizationAccessService;

    @Test
    @DisplayName("소속 조직과 요청한 조직이 같으면 조직 정보를 돌려준다")
    void verifyOrganization() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);

        // when
        MemberOrganizationResponse membership =
                organizationAccessService.verifyOrganization(ACCOUNT_UUID, ORGANIZATION_ID);

        // then
        assertAll(
                () -> assertEquals(ACCOUNT_UUID, membership.accountUuid()),
                () -> assertEquals(ORGANIZATION_ID, membership.organizationId()),
                () -> assertEquals(OrganizationRole.ORG_MEMBER, membership.organizationRole())
        );
    }

    @Test
    @DisplayName("다른 조직의 자원을 요청하면 거부한다")
    void verifyOrganizationDeniesOtherOrganization() {
        // given
        stubMembership(OrganizationRole.ORG_OWNER);

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyOrganization(ACCOUNT_UUID, 99L)
        );

        // then
        assertEquals(ErrorCode.ORGANIZATION_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("소속된 조직이 없으면 거부한다")
    void verifyOrganizationDeniesWithoutMembership() {
        // given
        when(memberOrganizationLookup.find(ACCOUNT_UUID)).thenReturn(Optional.empty());

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyOrganization(ACCOUNT_UUID, ORGANIZATION_ID)
        );

        // then
        assertEquals(ErrorCode.MEMBER_ORG_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("허용된 역할이면 통과한다")
    void verifyRole() {
        // given
        stubMembership(OrganizationRole.ORG_OWNER);

        // when
        MemberOrganizationResponse membership = organizationAccessService.verifyRole(
                ACCOUNT_UUID,
                ORGANIZATION_ID,
                OrganizationRole.ORG_BOSS,
                OrganizationRole.ORG_OWNER
        );

        // then
        assertEquals(OrganizationRole.ORG_OWNER, membership.organizationRole());
    }

    @Test
    @DisplayName("허용되지 않은 역할이면 거부한다")
    void verifyRoleDeniesUnlistedRole() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyRole(
                        ACCOUNT_UUID,
                        ORGANIZATION_ID,
                        OrganizationRole.ORG_BOSS,
                        OrganizationRole.ORG_OWNER
                )
        );

        // then
        assertEquals(ErrorCode.ORGANIZATION_ROLE_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("보스는 상태를 바꾸는 요청을 할 수 있다")
    void verifyOwnerOrBossAllowsBoss() {
        // given
        stubMembership(OrganizationRole.ORG_BOSS);

        // when
        MemberOrganizationResponse membership =
                organizationAccessService.verifyOwnerOrBoss(ACCOUNT_UUID, ORGANIZATION_ID);

        // then
        assertEquals(OrganizationRole.ORG_BOSS, membership.organizationRole());
    }

    @Test
    @DisplayName("오너는 상태를 바꾸는 요청을 할 수 있다")
    void verifyOwnerOrBossAllowsOwner() {
        // given
        stubMembership(OrganizationRole.ORG_OWNER);

        // when
        MemberOrganizationResponse membership =
                organizationAccessService.verifyOwnerOrBoss(ACCOUNT_UUID, ORGANIZATION_ID);

        // then
        assertEquals(OrganizationRole.ORG_OWNER, membership.organizationRole());
    }

    @Test
    @DisplayName("일반 조직원은 상태를 바꾸는 요청을 할 수 없다")
    void verifyOwnerOrBossDeniesMember() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyOwnerOrBoss(ACCOUNT_UUID, ORGANIZATION_ID)
        );

        // then
        assertEquals(ErrorCode.ORGANIZATION_ROLE_FORBIDDEN, exception.getErrorCode());
    }

    @Test
    @DisplayName("일반 조직원도 같은 조직이면 조회는 할 수 있다")
    void verifyOrganizationAllowsMemberForRead() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);

        // when
        MemberOrganizationResponse membership =
                organizationAccessService.verifyOrganization(ACCOUNT_UUID, ORGANIZATION_ID);

        // then
        assertEquals(OrganizationRole.ORG_MEMBER, membership.organizationRole());
    }

    @Test
    @DisplayName("인벤토리 호출이 실패하면 통과시키지 않고 예외를 그대로 올린다")
    void verifyOrganizationFailsClosed() {
        // given
        when(memberOrganizationLookup.find(ACCOUNT_UUID))
                .thenThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "호출 실패"));

        // when & then
        assertThrows(
                ApiException.class,
                () -> organizationAccessService.verifyOrganization(ACCOUNT_UUID, ORGANIZATION_ID)
        );
    }

    @Test
    @DisplayName("구역이 소속 조직의 것이면 통과한다")
    void verifyZone() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);
        stubZoneLocation(ORGANIZATION_ID);

        // when
        MemberOrganizationResponse membership =
                organizationAccessService.verifyZone(ACCOUNT_UUID, ORGANIZATION_ID, ZONE_ID);

        // then
        assertEquals(ORGANIZATION_ID, membership.organizationId());
    }

    @Test
    @DisplayName("소속 조직은 맞아도 다른 조직의 구역이면 거부한다")
    void verifyZoneDeniesOtherOrganizationZone() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);
        stubZoneLocation(99L);

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyZone(ACCOUNT_UUID, ORGANIZATION_ID, ZONE_ID)
        );

        // then
        assertEquals(ErrorCode.ZONE_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("존재하지 않는 구역이면 거부한다")
    void verifyZoneDeniesUnknownZone() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);
        when(zoneLookup.findLocation(ZONE_ID)).thenReturn(Optional.empty());

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyZone(ACCOUNT_UUID, ORGANIZATION_ID, ZONE_ID)
        );

        // then
        assertEquals(ErrorCode.ZONE_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    @DisplayName("구역 조회가 실패하면 통과시키지 않고 예외를 그대로 올린다")
    void verifyZoneFailsClosed() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);
        when(zoneLookup.findLocation(ZONE_ID))
                .thenThrow(new ApiException(ErrorCode.EXTERNAL_API_ERROR, "호출 실패"));

        // when & then
        assertThrows(
                ApiException.class,
                () -> organizationAccessService.verifyZone(ACCOUNT_UUID, ORGANIZATION_ID, ZONE_ID)
        );
    }

    @Test
    @DisplayName("보스는 구역 상태를 바꾸는 요청을 할 수 있다")
    void verifyZoneOwnerOrBossAllowsBoss() {
        // given
        stubMembership(OrganizationRole.ORG_BOSS);
        stubZoneLocation(ORGANIZATION_ID);

        // when
        MemberOrganizationResponse membership =
                organizationAccessService.verifyZoneOwnerOrBoss(ACCOUNT_UUID, ORGANIZATION_ID, ZONE_ID);

        // then
        assertEquals(OrganizationRole.ORG_BOSS, membership.organizationRole());
    }

    @Test
    @DisplayName("역할이 부족하면 구역을 조회하기 전에 거부한다")
    void verifyZoneOwnerOrBossDeniesMemberBeforeZoneLookup() {
        // given
        stubMembership(OrganizationRole.ORG_MEMBER);

        // when
        OrganizationAccessDeniedException exception = assertThrows(
                OrganizationAccessDeniedException.class,
                () -> organizationAccessService.verifyZoneOwnerOrBoss(ACCOUNT_UUID, ORGANIZATION_ID, ZONE_ID)
        );

        // then
        assertAll(
                () -> assertEquals(ErrorCode.ORGANIZATION_ROLE_FORBIDDEN, exception.getErrorCode()),
                () -> verifyNoInteractions(zoneLookup)
        );
    }

    private void stubZoneLocation(Long organizationId) {
        when(zoneLookup.findLocation(ZONE_ID)).thenReturn(
                Optional.of(new ResolvedZoneResponse(organizationId, STORAGE_ID, ZONE_ID))
        );
    }

    private void stubMembership(OrganizationRole role) {
        when(memberOrganizationLookup.find(ACCOUNT_UUID)).thenReturn(
                Optional.of(new MemberOrganizationResponse(ACCOUNT_UUID, ORGANIZATION_ID, role))
        );
    }
}
