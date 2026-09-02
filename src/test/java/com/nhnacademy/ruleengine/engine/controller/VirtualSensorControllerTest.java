
package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.inventory.MemberOrganizationResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.OrganizationRole;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.virtual.SensorValue;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorStatus;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorValues;
import com.nhnacademy.ruleengine.engine.dto.virtual.request.VirtualSensorCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorCreateResponse;
import com.nhnacademy.ruleengine.engine.dto.virtual.response.VirtualSensorInfoResponse;
import com.nhnacademy.ruleengine.engine.exception.OrganizationAccessDeniedException;
import com.nhnacademy.ruleengine.engine.exception.VirtualSensorFlowException;
import com.nhnacademy.ruleengine.engine.service.OrganizationAccessService;
import com.nhnacademy.ruleengine.engine.service.VirtualSensorService;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.nhnacademy.ruleengine.engine.dto.sensor.SensorType.TEMPERATURE;
import static com.nhnacademy.ruleengine.engine.dto.virtual.GenerationMode.RANGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VirtualSensorController.class)
class VirtualSensorControllerTest {

    private final UUID accountUuid = UUID.randomUUID();
    private final Long organizationId = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VirtualSensorService virtualSensorService;

    @MockitoBean
    private OrganizationAccessService organizationAccessService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("POST - 가상센서 생성 성공")
    void createVirtualSensor() throws Exception {

        // given
        VirtualSensorCreateRequest request = createRequest();

        authenticate(accountUuid);

        when(organizationAccessService.verifyOwnerOrBoss(
                accountUuid,
                organizationId
        )).thenReturn(createAccessResponse());

        VirtualSensorCreateResponse response =
                VirtualSensorCreateResponse.from("testEui");

        when(virtualSensorService.createVirtualSensor(
                eq(organizationId),
                any(VirtualSensorCreateRequest.class)
        )).thenReturn(response);

        // when & then
        mockMvc.perform(
                        post(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk());

        verify(organizationAccessService)
                .verifyOwnerOrBoss(accountUuid, organizationId);

        verify(virtualSensorService)
                .createVirtualSensor(
                        eq(organizationId),
                        any(VirtualSensorCreateRequest.class)
                );
    }

    @Test
    @DisplayName("POST - 가상센서 생성 실패 - 조직원이지만 접근 권한 없음")
    void createVirtualSensorWithoutAccess() throws Exception {

        // given
        VirtualSensorCreateRequest request = createRequest();

        authenticate(accountUuid);

        when(organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId))
                .thenThrow(new OrganizationAccessDeniedException(
                        ErrorCode.ORGANIZATION_ROLE_FORBIDDEN
                ));

        // when & then
        mockMvc.perform(
                        post(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());

        verify(organizationAccessService)
                .verifyOwnerOrBoss(accountUuid, organizationId);

        verify(virtualSensorService, never())
                .createVirtualSensor(
                        eq(organizationId),
                        any(VirtualSensorCreateRequest.class)
                );
    }

    @Test
    @DisplayName("Post - 가상센서 생성 실패 - 해당 조직 소속이 아님")
    void createVirtualSensorWithOrganizationNotOwned() throws Exception {

        // given
        VirtualSensorCreateRequest request = createRequest();

        authenticate(accountUuid);

        when(organizationAccessService.verifyOwnerOrBoss(accountUuid, organizationId))
                .thenThrow(new OrganizationAccessDeniedException(
                        ErrorCode.ORGANIZATION_ACCESS_DENIED
                ));

        // when & then
        mockMvc.perform(
                        post(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Post - 가상센서 생성 실패 - 이미 존재하는 가상센서")
    void createVirtualSensorDuplicate() throws Exception {

        // given
        VirtualSensorCreateRequest request = createRequest();

        authenticate(accountUuid);

        when(organizationAccessService.verifyOwnerOrBoss(
                accountUuid,
                organizationId
        )).thenReturn(createAccessResponse());

        when(virtualSensorService.createVirtualSensor(
                eq(organizationId),
                any(VirtualSensorCreateRequest.class)
        )).thenThrow(new VirtualSensorFlowException(
                ErrorCode.VIRTUAL_SENSOR_CONFIG_EXISTS
        ));

        // when & then
        mockMvc.perform(
                        post(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isConflict());

        verify(organizationAccessService)
                .verifyOwnerOrBoss(accountUuid, organizationId);

        verify(virtualSensorService)
                .createVirtualSensor(
                        eq(organizationId),
                        any(VirtualSensorCreateRequest.class)
                );
    }

    @Test
    @DisplayName("GET - 조직의 가상센서 목록 조회 성공")
    void getVirtualSensorsByOrganization() throws Exception {

        // given
        authenticate(accountUuid);

        when(virtualSensorService.getVirtualSensors(organizationId))
                .thenReturn(Collections.emptyList());

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET - 조직의 가상센서 목록 조회 실패 - 해당 조직원이 아님")
    void getVirtualSensorsByOrganizationWithoutAccess() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenThrow(new OrganizationAccessDeniedException(
                        ErrorCode.ORGANIZATION_ACCESS_DENIED
                ));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET - 해당 조직의 device-eui 의 센서 정보 조회 성공")
    void getVirtualSensorsByDeviceEui() throws Exception {

        // given
        String deviceEui = "device-eui";

        authenticate(accountUuid);

        VirtualSensorValues virtualSensorValues = new VirtualSensorValues(
                Map.of(
                        TEMPERATURE,
                        new SensorValue(
                                RANGE,
                                15.0,
                                25.0,
                                null,
                                null
                        )
                )
        );

        VirtualSensorInfoResponse infoResponse =
                new VirtualSensorInfoResponse(
                        true,
                        deviceEui,
                        30L,
                        virtualSensorValues,
                        VirtualSensorStatus.ACTIVE,
                        1L
                );

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenReturn(createAccessResponse());

        when(virtualSensorService.getVirtualSensor(organizationId, deviceEui))
                .thenReturn(infoResponse);

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors/{device-eui}",
                                organizationId,
                                deviceEui
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(organizationAccessService)
                .verifyOrganization(accountUuid, organizationId);

        verify(virtualSensorService)
                .getVirtualSensor(organizationId, deviceEui);
    }

    @Test
    @DisplayName("GET - 해당 조직의 device-eui 의 센서 정보 조회 실패 - 해당 조직원이 아님")
    void getVirtualSensorsByDeviceEuiWithoutAccess() throws Exception {

        // given
        String deviceEui = "device-eui";

        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenThrow(new OrganizationAccessDeniedException(
                        ErrorCode.ORGANIZATION_ACCESS_DENIED
                ));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors/{device-eui}",
                                organizationId,
                                deviceEui
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        verify(organizationAccessService).verifyOrganization(accountUuid, organizationId);

        verify(virtualSensorService, never()).getVirtualSensor(organizationId, deviceEui);
    }

    private @NonNull VirtualSensorInfoResponse getVirtualSensorInfoResponse(String deviceEui) {
        VirtualSensorValues virtualSensorValues = new VirtualSensorValues(
                Map.of(
                        TEMPERATURE,
                        new SensorValue(
                                RANGE,
                                15.0,
                                25.0,
                                null,
                                null
                        )
                )
        );

        VirtualSensorInfoResponse infoResponse = new VirtualSensorInfoResponse(
                true,
                deviceEui,
                30L,
                virtualSensorValues,
                VirtualSensorStatus.ACTIVE,
                1L
        );
        return infoResponse;
    }

    private MemberOrganizationResponse createAccessResponse() {
        return new MemberOrganizationResponse(
                accountUuid,
                organizationId,
                OrganizationRole.ORG_BOSS
        );
    }

    private VirtualSensorCreateRequest createRequest() {

        Map<SensorType, SensorValue> values = Map.of(
                TEMPERATURE,
                new SensorValue(
                        RANGE,
                        15.0,
                        25.0,
                        null,
                        null
                )
        );

        return new VirtualSensorCreateRequest(
                "testEui",
                5L,
                new VirtualSensorValues(values)
        );
    }

    private void authenticate(UUID accountUuid) {

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(accountUuid.toString())
                .build();

        JwtAuthenticationToken authentication =
                new JwtAuthenticationToken(jwt);

        authentication.setAuthenticated(true);

        SecurityContextHolder.getContext()
                .setAuthentication(authentication);
    }
}

