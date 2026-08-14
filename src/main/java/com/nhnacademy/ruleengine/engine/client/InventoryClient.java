package com.nhnacademy.ruleengine.engine.client;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

// 인벤토리 서비스의 내부 전용 API(/api/core/internal/**)를 호출한다.
// 내부 API는 게이트웨이의 JWT 필터 대상이라, 게이트웨이를 거치지 않고 인벤토리로 직접 호출한다.
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

}
