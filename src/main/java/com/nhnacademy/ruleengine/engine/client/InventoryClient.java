package com.nhnacademy.ruleengine.engine.client;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

// 인벤토리 서비스의 내부 전용 API(/api/core/internal/**)를 호출한다.
@Slf4j
@Component
public class InventoryClient {

    private static final String INVENTORY_URL = "/api/core/internal";

    private final ApiClient apiClient;
    private final String baseUrl;

    public InventoryClient(
            ApiClient apiClient,
            @Value("${rule-engine.inventory.base-url}") String baseUrl
    ) {
        this.apiClient = apiClient;
        this.baseUrl = baseUrl;
    }


    public ThresholdPolicyDto getThresholdPolicy(Long organizationId, Long storageId, Long zoneId) {

        // 추후 API 요청로직으로 변경

        return new ThresholdPolicyDto(
                organizationId,
                storageId,
                zoneId,
                Map.of(
                        SensorType.TEMPERATURE.value(), new ThresholdRange(20.0, 30.0),
                        SensorType.HUMIDITY.value(), new ThresholdRange(10.0, 70.0),
                        SensorType.ILLUMINATION.value(), new ThresholdRange(0.0, 100.0)
                ), 1
        );
    }

    // deviceEui로 센서가 설치된 위치(조직/창고/구역)를 조회한다.
    public ResolvedZoneResponse getZoneResponse(
            String deviceEui
    ) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl + INVENTORY_URL + "/devices/location")
                .queryParam("device-eui", deviceEui)
                .encode()
                .toUriString();

        return apiClient.get(url, ResolvedZoneResponse.class);
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

}
