package com.nhnacademy.ruleengine.engine.client;

import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.dto.rule.ThresholdPolicyDto.ThresholdRange;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Component
public class InventoryClient {

    private final RestClient restClient;
    private final SectionCatalog sectionCatalog;

    public InventoryClient(
            RestClient.Builder builder,
            @Value("${rule-engine.inventory.base-url}") String baseUrl,
            SectionCatalog sectionCatalog
    ) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .build();
        this.sectionCatalog = sectionCatalog;
    }


    public ThresholdPolicyDto getThresholdPolicy(Long organizationId, Long storageId, Long zoneId) {

        // 추후 API 요청로직으로 변경

        return new ThresholdPolicyDto(
                organizationId,
                storageId,
                zoneId,
                Map.of(
                        SensorType.TEMPERATURE.value(), new ThresholdRange(4.0, 5.0),
                        SensorType.HUMIDITY.value(), new ThresholdRange(1.0, 2.0),
                        SensorType.ILLUMINATION.value(), new ThresholdRange(0.0, 100.0)
                ), 1
        );
    }

    public ResolvedZoneResponse getZoneResponse(
            String deviceEui
    ) {
        // 추후 API 요청로직으로 변경
        SectionCatalog.ResolvedSection section = sectionCatalog
                .resolveSection(deviceEui)
                .orElseThrow(() -> new ApiException(
                        ErrorCode.SECTION_NOT_FOUND,
                        "섹션을 찾을 수 없습니다."
                ));

        return new ResolvedZoneResponse(
                section.organizationId(),
                section.storageId(),
                section.sectionId()
        );
    }

}