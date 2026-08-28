package com.nhnacademy.ruleengine.global.client;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.inventory.MemberOrganizationResponse;
import com.nhnacademy.ruleengine.engine.dto.inventory.ThresholdSpecResponse;
import com.nhnacademy.ruleengine.engine.dto.notification.EnvironmentEventCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

// 인벤토리 서비스의 내부 전용 API(/api/core/internal/**)를 호출한다.
@Slf4j
@Component
public class InventoryClient {

    private static final String INVENTORY_URL = "/api/core/internal";

    private static final ParameterizedTypeReference<ApiResponse<List<ThresholdSpecResponse>>> THRESHOLD_SPEC_LIST =
            new ParameterizedTypeReference<>() {
            };

    private final ApiClient apiClient;
    private final String baseUrl;

    public InventoryClient(
            ApiClient apiClient,
            @Value("${rule-engine.inventory.base-url}") String baseUrl
    ) {
        this.apiClient = apiClient;
        this.baseUrl = baseUrl;
    }


    // 구역에 설정된 센서타입별 임계값을 조회한다. 설정이 없으면 범위가 빈 정책을 돌려준다.
    public ThresholdPolicyDto getThresholdPolicy(Long zoneId) {
        List<ThresholdSpecResponse> specs = apiClient.get(
                baseUrl + INVENTORY_URL + "/zones/" + zoneId + "/zone-threshold",
                THRESHOLD_SPEC_LIST
        );

        return new ThresholdPolicyDto(
                toRanges(zoneId, specs == null ? List.of() : specs)
        );
    }

    // deviceEui로 센서가 설치된 위치(조직/창고/구역)를 조회한다.
    // 등록되지 않은 기기면 빈 값이고, 인벤토리 장애는 예외로 올라온다.
    public Optional<ResolvedZoneResponse> findZoneResponse(
            String deviceEui
    ) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + INVENTORY_URL + "/devices/location")
                .queryParam("device-eui", deviceEui)
                .encode()
                .toUriString();

        return apiClient.find(url, ResolvedZoneResponse.class);
    }

    // zoneId로 그 구역이 속한 위치(조직/창고/구역)를 조회한다.
    // 경로의 구역이 요청자의 조직 소속인지 검증할 때 쓴다.
    // 없는 구역이면 빈 값이고, 인벤토리 장애는 예외로 올라온다.
    public Optional<ResolvedZoneResponse> findZoneLocation(Long zoneId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + INVENTORY_URL + "/zones/" + zoneId + "/location")
                .encode()
                .toUriString();

        return apiClient.find(url, ResolvedZoneResponse.class);
    }

    // 계정 UUID로 소속 조직과 조직 역할을 조회한다.
    // 조직에 속하지 않은 계정이면 빈 값이고, 인벤토리 장애는 예외로 올라온다.
    public Optional<MemberOrganizationResponse> findMemberOrganization(UUID accountUuid) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + INVENTORY_URL + "/members/" + accountUuid + "/organization")
                .encode()
                .toUriString();

        return apiClient.find(url, MemberOrganizationResponse.class);
    }

    // 구역의 환경 상태를 갱신한다.
    public void updateEnvStatus(Long zoneId, EnvStatus envStatus) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + INVENTORY_URL + "/zones/" + zoneId + "/env-status")
                .queryParam("env-status", envStatus.name())
                .encode()
                .toUriString();

        apiClient.put(url);
    }

    // 웹에 알림을 발송한다
    public void createEnvironmentEvent(EnvironmentEventCreateRequest request) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + INVENTORY_URL + "/environment-events")
                .encode()
                .toUriString();
        apiClient.post(url, request);
    }

    private Map<String, ThresholdRange> toRanges(Long zoneId, List<ThresholdSpecResponse> specs) {
        Map<String, ThresholdRange> ranges = new LinkedHashMap<>();

        for (ThresholdSpecResponse spec : specs) {
            // 인벤토리는 "TEMPERATURE", 룰 엔진은 "temperature"를 키로 쓴다.
            Optional<SensorType> sensorType = SensorType.findByValue(spec.sensorTypeName());

            if (sensorType.isEmpty()) {
                log.warn("지원하지 않는 센서 종류의 임계값이라 건너뜁니다. zoneId={}, sensorTypeName={}",
                        zoneId, spec.sensorTypeName());
                continue;
            }

            ranges.put(
                    sensorType.get().value(),
                    new ThresholdRange(
                            toDouble(spec.minValue()),
                            toDouble(spec.maxValue()),
                            spec.alertDuration()
                    )
            );
        }

        return ranges;
    }

    private Double toDouble(BigDecimal value) {
        return value == null ? null : value.doubleValue();
    }

}
