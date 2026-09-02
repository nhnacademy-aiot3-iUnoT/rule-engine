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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nhnacademy.ruleengine.engine.dto.sensor.SensorType.TEMPERATURE;
import static com.nhnacademy.ruleengine.engine.dto.virtual.GenerationMode.RANGE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VirtualSensorController.class)
class VirtualSensorControllerTest extends RestDocsSupport {

    private final UUID accountUuid = UUID.randomUUID();
    private final Long organizationId = 1L;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VirtualSensorService virtualSensorService;

    @MockitoBean
    private OrganizationAccessService organizationAccessService;

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
                .andExpect(status().isOk())
                .andDo(document("virtual-sensor-create",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID")
                        ),
                        requestFields(
                                fieldWithPath("deviceEui").description("가상센서 디바이스 EUI"),
                                fieldWithPath("measurementIntervalSeconds").description("측정 주기(초)"),
                                subsectionWithPath("virtualSensorValues")
                                        .description("센서 종류별 값 생성 설정")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data.deviceEui").description("생성된 가상센서 디바이스 EUI"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

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
                .andExpect(status().isForbidden())
                .andDo(document("virtual-sensor-create-forbidden",
                        responseFields(
                                fieldWithPath("success").description("성공 여부(false)"),
                                fieldWithPath("data").type(JsonFieldType.OBJECT)
                                        .description("응답 데이터(실패 시 null)").optional(),
                                fieldWithPath("error.code").description("에러 코드"),
                                fieldWithPath("error.message").description("에러 메시지"),
                                fieldWithPath("error.fieldErrors").description("필드별 에러 목록"),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

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

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenReturn(createAccessResponse());

        when(virtualSensorService.getVirtualSensors(organizationId))
                .thenReturn(List.of(getVirtualSensorInfoResponse("device-eui")));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/virtual-sensors",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("virtual-sensor-list",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data[].registered").description("구역 센서로 등록되었는지 여부"),
                                fieldWithPath("data[].deviceEui").description("디바이스 EUI"),
                                fieldWithPath("data[].measurementIntervalSeconds").description("측정 주기(초)"),
                                subsectionWithPath("data[].virtualSensorValues")
                                        .description("센서 종류별 값 생성 설정"),
                                fieldWithPath("data[].status").description("가상센서 상태(ACTIVE/INACTIVE)"),
                                fieldWithPath("data[].zoneId").description("등록된 구역 ID(미등록이면 null)").optional(),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));
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

        VirtualSensorInfoResponse infoResponse = getVirtualSensorInfoResponse(deviceEui);

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
                .andExpect(status().isOk())
                .andDo(document("virtual-sensor-detail",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID"),
                                parameterWithName("device-eui").description("가상센서 디바이스 EUI")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data.registered").description("구역 센서로 등록되었는지 여부"),
                                fieldWithPath("data.deviceEui").description("디바이스 EUI"),
                                fieldWithPath("data.measurementIntervalSeconds").description("측정 주기(초)"),
                                subsectionWithPath("data.virtualSensorValues")
                                        .description("센서 종류별 값 생성 설정"),
                                fieldWithPath("data.status").description("가상센서 상태(ACTIVE/INACTIVE)"),
                                fieldWithPath("data.zoneId").description("등록된 구역 ID(미등록이면 null)").optional(),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

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

    private VirtualSensorInfoResponse getVirtualSensorInfoResponse(String deviceEui) {
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

        return new VirtualSensorInfoResponse(
                true,
                deviceEui,
                30L,
                virtualSensorValues,
                VirtualSensorStatus.ACTIVE,
                1L
        );
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
}
