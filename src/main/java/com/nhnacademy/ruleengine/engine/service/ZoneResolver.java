package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

// deviceEui로 인벤토리에서 구역 정보를 조회한다.
// 미등록 센서(404)든 인벤토리 장애든 조회에 실패하면 빈 값을 돌려주고, 호출 측은 해당 메시지를 버린다.
@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneResolver {

    private final InventoryClient inventoryClient;

    public Optional<ResolvedZoneResponse> resolve(
            String deviceEui
    ) {
        try {
            return Optional.ofNullable(
                    inventoryClient.getZoneResponse(deviceEui)
            );

        } catch (ApiException e) {
            log.warn(
                    "구역 정보 조회에 실패했습니다. deviceEui={}, errorCode={}, reason={}",
                    deviceEui,
                    e.getErrorCode(),
                    e.getMessage()
            );

            return Optional.empty();
        }
    }
}
