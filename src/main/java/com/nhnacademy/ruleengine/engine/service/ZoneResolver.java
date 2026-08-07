package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog.ResolvedSection;
import com.nhnacademy.ruleengine.engine.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ZoneResolver {

    private final InventoryClient inventoryClient;

    public Optional<ResolvedSection> resolve(
            String applicationName,
            String location,
            String point
    ) {
        try {
            ResolvedZoneResponse response =
                    inventoryClient.getZoneResponse(
                            applicationName,
                            location,
                            point
                    );

            return Optional.of(
                    new ResolvedSection(
                            response.organizationId(),
                            response.storageId(),
                            response.zoneId()
                    )
            );

        } catch (ApiException e) {
            return Optional.empty();
        }
    }
}
