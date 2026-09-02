package com.nhnacademy.ruleengine.engine.controller;

import com.nhnacademy.ruleengine.engine.dto.environment.StorageDailySummary;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.engine.service.StorageDailySummaryArchiveService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EnvironmentReportController.class)
class EnvironmentReportControllerTest extends RestDocsSupport {

    private final UUID accountUuid = UUID.randomUUID();
    private final Long organizationId = 1L;
    private final Long storageId = 100L;

    @MockitoBean
    private StorageDailySummaryArchiveService storageDailySummaryArchiveService;

    @MockitoBean
    private SensorInfluxService sensorInfluxService;

    @Test
    @DisplayName("GET - 저장소의 구역별 센서 최신값 조회 성공")
    void findLatestSensors() throws Exception {

        // given
        authenticate(accountUuid);

        when(sensorInfluxService.findLatestByStorage(organizationId, storageId))
                .thenReturn(List.of(sensorPayload()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/internal/storages/{storage-id}/sensor-data/latest",
                                storageId
                        )
                                .param("organizationId", organizationId.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("internal-storage-sensor-latest",
                        pathParameters(
                                parameterWithName("storage-id").description("저장소 ID")
                        ),
                        queryParameters(
                                parameterWithName("organizationId").description("조직 ID")
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

        verify(sensorInfluxService).findLatestByStorage(organizationId, storageId);
    }

    @Test
    @DisplayName("GET - 저장소의 하루 요약 조회 성공 - 기간 미지정")
    void findDailySummariesWithoutPeriod() throws Exception {

        // given
        authenticate(accountUuid);

        when(storageDailySummaryArchiveService.findBetween(
                eq(storageId),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(List.of(dailySummary()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/internal/storages/{storage-id}/daily-summaries",
                                storageId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(storageDailySummaryArchiveService)
                .findBetween(eq(storageId), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    @DisplayName("GET - 저장소의 하루 요약 조회 성공 - from, to 지정")
    void findDailySummariesWithPeriod() throws Exception {

        // given
        authenticate(accountUuid);

        LocalDate from = LocalDate.of(2026, 8, 1);
        LocalDate to = LocalDate.of(2026, 8, 7);

        when(storageDailySummaryArchiveService.findBetween(storageId, from, to))
                .thenReturn(List.of(dailySummary()));

        // when & then
        mockMvc.perform(
                        get(
                                "/api/rule-engine/internal/storages/{storage-id}/daily-summaries",
                                storageId
                        )
                                .param("from", from.toString())
                                .param("to", to.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("internal-storage-daily-summaries",
                        pathParameters(
                                parameterWithName("storage-id").description("저장소 ID")
                        ),
                        queryParameters(
                                parameterWithName("from").description("조회 시작일(yyyy-MM-dd, 생략 시 to-6일)"),
                                parameterWithName("to").description("조회 종료일(yyyy-MM-dd, 생략 시 어제)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data[].storageId").description("저장소 ID"),
                                fieldWithPath("data[].date").description("요약 대상 날짜"),
                                fieldWithPath("data[].zones").description("구역별 요약 목록(데이터 있는 구역만)"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        verify(storageDailySummaryArchiveService).findBetween(storageId, from, to);
    }

    @Test
    @DisplayName("POST - 하루 요약 적재 수동 실행 성공 - 날짜 미지정")
    void rollupWithoutDate() throws Exception {

        // given
        authenticate(accountUuid);

        when(storageDailySummaryArchiveService.rollup(any(LocalDate.class)))
                .thenReturn(3);

        // when & then
        mockMvc.perform(
                        post("/api/rule-engine/internal/daily-summary-rollups")
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());

        verify(storageDailySummaryArchiveService).rollup(any(LocalDate.class));
    }

    @Test
    @DisplayName("POST - 하루 요약 적재 수동 실행 성공 - 날짜 지정")
    void rollupWithDate() throws Exception {

        // given
        authenticate(accountUuid);

        LocalDate date = LocalDate.of(2026, 8, 15);

        when(storageDailySummaryArchiveService.rollup(date))
                .thenReturn(5);

        // when & then
        mockMvc.perform(
                        post("/api/rule-engine/internal/daily-summary-rollups")
                                .queryParam("date", date.toString())
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andDo(document("internal-daily-summary-rollup",
                        queryParameters(
                                parameterWithName("date").description("적재 대상 날짜(yyyy-MM-dd, 생략 시 어제)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("성공 여부"),
                                fieldWithPath("data.date").description("적재한 날짜"),
                                fieldWithPath("data.savedZoneCount").description("적재된 구역 수"),
                                fieldWithPath("error").type(JsonFieldType.OBJECT)
                                        .description("에러 정보(성공 시 null)").optional(),
                                fieldWithPath("timestamp").description("응답 생성 시각")
                        )
                ));

        verify(storageDailySummaryArchiveService).rollup(date);
    }

    private SensorPayload sensorPayload() {
        return new SensorPayload(
                organizationId,
                "device-eui",
                storageId,
                10L,
                "temperature",
                21.5,
                "℃",
                Instant.now().toString()
        );
    }

    private StorageDailySummary dailySummary() {
        return new StorageDailySummary(
                storageId,
                LocalDate.of(2026, 8, 1),
                List.of()
        );
    }
}
