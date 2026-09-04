package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.inventory.MemberOrganizationResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.OrganizationRole;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.query.SensorHistoryResponse;
import com.nhnacademy.ruleengine.engine.exception.OrganizationAccessDeniedException;
import com.nhnacademy.ruleengine.engine.service.OrganizationAccessService;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SensorDataQueryController.class)
class SensorDataQueryControllerTest extends RestDocsSupport {

    private final UUID accountUuid = UUID.randomUUID();
    private final Long organizationId = 1L;
    private final Long zoneId = 10L;
    private final Long storageId = 100L;

    @MockitoBean
    private SensorInfluxService sensorInfluxService;

    @MockitoBean
    private OrganizationAccessService organizationAccessService;

    @Test
    @DisplayName("GET - 구역의 센서별 최신값 조회 성공")
    void findLatestByZone() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyZone(accountUuid, organizationId, zoneId))
                .thenReturn(createAccessResponse());

        when(sensorInfluxService.findLatestByZone(zoneId))
                .thenReturn(List.of(sensorPayload()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/zones/{zone-id}/sensor-data/latest",
                                organizationId,
                                zoneId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("zone-sensor-latest",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID"),
                                parameterWithName("zone-id").description("구역 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data[].organizationId").description("조직 ID"),
                                fieldWithPath("data[].deviceEui").description("디바이스 EUI"),
                                fieldWithPath("data[].storageId").description("저장소 ID"),
                                fieldWithPath("data[].zoneId").description("구역 ID"),
                                fieldWithPath("data[].sensorType").description("센서 종류"),
                                fieldWithPath("data[].value").description("측정값"),
                                fieldWithPath("data[].unit").description("단위"),
                                fieldWithPath("data[].measuredAt").description("측정 시각(ISO-8601)"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        verify(organizationAccessService).verifyZone(accountUuid, organizationId, zoneId);
        verify(sensorInfluxService).findLatestByZone(zoneId);
    }

    @Test
    @DisplayName("GET - 구역의 센서별 최신값 조회 실패 - 다른 조직의 구역")
    void findLatestByZoneWithoutAccess() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyZone(accountUuid, organizationId, zoneId))
                .thenThrow(new OrganizationAccessDeniedException(ErrorCode.ZONE_ACCESS_DENIED));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/zones/{zone-id}/sensor-data/latest",
                                organizationId,
                                zoneId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden())
                .andDo(document("zone-sensor-latest-forbidden",
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

        verify(organizationAccessService).verifyZone(accountUuid, organizationId, zoneId);
        verify(sensorInfluxService, never()).findLatestByZone(zoneId);
    }

    @Test
    @DisplayName("GET - 구역의 센서 이력 조회 성공")
    void findHistoryByZone() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyZone(accountUuid, organizationId, zoneId))
                .thenReturn(createAccessResponse());

        when(sensorInfluxService.findHistoryByZone(
                eq(zoneId),
                any(),
                any(),
                any(),
                any()
        )).thenReturn(List.of(
                new SensorHistoryResponse("temperature", "℃", Instant.now(), 21.5)
        ));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/zones/{zone-id}/sensor-data/history",
                                organizationId,
                                zoneId
                        )
                                .param("sensorType", "temperature")
                                .param("from", "2026-08-01T00:00:00Z")
                                .param("to", "2026-08-02T00:00:00Z")
                                .param("window", "30m")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("zone-sensor-history",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID"),
                                parameterWithName("zone-id").description("구역 ID")
                        ),
                        queryParameters(
                                parameterWithName("sensorType").description("센서 종류(생략 시 전체)").optional(),
                                parameterWithName("from").description("조회 시작 시각(ISO-8601, 생략 시 to-24h)").optional(),
                                parameterWithName("to").description("조회 종료 시각(ISO-8601, 생략 시 현재)").optional(),
                                parameterWithName("window").description("집계 단위(예: 30m, 1h, 생략 시 30m)").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data[].sensorType").description("센서 종류"),
                                fieldWithPath("data[].unit").description("단위"),
                                fieldWithPath("data[].time").description("집계 구간 시각(ISO-8601)"),
                                fieldWithPath("data[].value").description("집계값"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        verify(organizationAccessService).verifyZone(accountUuid, organizationId, zoneId);
        verify(sensorInfluxService).findHistoryByZone(eq(zoneId), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET - 구역의 센서 이력 조회 실패 - 다른 조직의 구역")
    void findHistoryByZoneWithoutAccess() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyZone(accountUuid, organizationId, zoneId))
                .thenThrow(new OrganizationAccessDeniedException(ErrorCode.ZONE_ACCESS_DENIED));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/zones/{zone-id}/sensor-data/history",
                                organizationId,
                                zoneId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        verify(sensorInfluxService, never())
                .findHistoryByZone(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET - 저장소의 센서별 최신값 조회 성공")
    void findLatestByStorage() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenReturn(createAccessResponse());

        when(sensorInfluxService.findLatestByStorage(organizationId, storageId))
                .thenReturn(List.of(sensorPayload()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/storages/{storage-id}/sensor-data/latest",
                                organizationId,
                                storageId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("storage-sensor-latest",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID"),
                                parameterWithName("storage-id").description("저장소 ID")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data[].organizationId").description("조직 ID"),
                                fieldWithPath("data[].deviceEui").description("디바이스 EUI"),
                                fieldWithPath("data[].storageId").description("저장소 ID"),
                                fieldWithPath("data[].zoneId").description("구역 ID"),
                                fieldWithPath("data[].sensorType").description("센서 종류"),
                                fieldWithPath("data[].value").description("측정값"),
                                fieldWithPath("data[].unit").description("단위"),
                                fieldWithPath("data[].measuredAt").description("측정 시각(ISO-8601)"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        verify(organizationAccessService).verifyOrganization(accountUuid, organizationId);
        verify(sensorInfluxService).findLatestByStorage(organizationId, storageId);
    }

    @Test
    @DisplayName("GET - 저장소의 센서별 최신값 조회 실패 - 소속되지 않은 조직")
    void findLatestByStorageWithoutAccess() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenThrow(new OrganizationAccessDeniedException(ErrorCode.ORGANIZATION_ACCESS_DENIED));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/storages/{storage-id}/sensor-data/latest",
                                organizationId,
                                storageId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        verify(sensorInfluxService, never()).findLatestByStorage(organizationId, storageId);
    }

    @Test
    @DisplayName("GET - 조직의 센서별 최신값 조회 성공")
    void findLatestByOrganization() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenReturn(createAccessResponse());

        when(sensorInfluxService.findLatestByOrganization(organizationId, "temperature"))
                .thenReturn(List.of(sensorPayload()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/sensor-data/latest",
                                organizationId
                        )
                                .param("sensorType", "temperature")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("organization-sensor-latest",
                        pathParameters(
                                parameterWithName("organization-id").description("조직 ID")
                        ),
                        queryParameters(
                                parameterWithName("sensorType").description("센서 종류(생략 시 전체)").optional()
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data[].organizationId").description("조직 ID"),
                                fieldWithPath("data[].deviceEui").description("디바이스 EUI"),
                                fieldWithPath("data[].storageId").description("저장소 ID"),
                                fieldWithPath("data[].zoneId").description("구역 ID"),
                                fieldWithPath("data[].sensorType").description("센서 종류"),
                                fieldWithPath("data[].value").description("측정값"),
                                fieldWithPath("data[].unit").description("단위"),
                                fieldWithPath("data[].measuredAt").description("측정 시각(ISO-8601)"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        verify(organizationAccessService).verifyOrganization(accountUuid, organizationId);
        verify(sensorInfluxService).findLatestByOrganization(organizationId, "temperature");
    }

    @Test
    @DisplayName("GET - 조직의 센서별 최신값 조회 성공 - sensorType 미지정")
    void findLatestByOrganizationWithoutSensorType() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenReturn(createAccessResponse());

        when(sensorInfluxService.findLatestByOrganization(organizationId, null))
                .thenReturn(List.of(sensorPayload()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/sensor-data/latest",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(sensorInfluxService).findLatestByOrganization(organizationId, null);
    }

    @Test
    @DisplayName("GET - 조직의 센서별 최신값 조회 실패 - 소속되지 않은 조직")
    void findLatestByOrganizationWithoutAccess() throws Exception {

        // given
        authenticate(accountUuid);

        when(organizationAccessService.verifyOrganization(accountUuid, organizationId))
                .thenThrow(new OrganizationAccessDeniedException(ErrorCode.ORGANIZATION_ACCESS_DENIED));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/organizations/{organization-id}/sensor-data/latest",
                                organizationId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isForbidden());

        verify(sensorInfluxService, never()).findLatestByOrganization(any(), any());
    }

    private SensorPayload sensorPayload() {
        return new SensorPayload(
                organizationId,
                "device-eui",
                storageId,
                zoneId,
                "temperature",
                21.5,
                "℃",
                Instant.now().toString()
        );
    }

    private MemberOrganizationResponse createAccessResponse() {
        return new MemberOrganizationResponse(
                accountUuid,
                organizationId,
                OrganizationRole.ORG_BOSS
        );
    }
}
