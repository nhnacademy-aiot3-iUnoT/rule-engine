package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.ZoneActivationResponse;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ZoneResolver {

    private final CachedZoneLookup cachedZoneLookup;

    public Optional<ResolvedZoneResponse> resolve(
            String deviceEui
    ) {
        try {
            return cachedZoneLookup.find(deviceEui);

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

    /**
     * 운영 중인 구역에 등록된 기기만 해석한다.
     * <p>
     * 구역이나 저장소를 비활성으로 바꾸면 그 구역의 데이터는 여기서 끊기므로,
     * 저장·룰 판정·환경 상태 갱신·이벤트·알림이 한꺼번에 멈춘다.
     */
    public Optional<ResolvedZoneResponse> resolveActive(
            String deviceEui
    ) {
        return resolve(deviceEui)
                .filter(zone -> {
                    if (isZoneActive(zone.zoneId())) {
                        return true;
                    }

                    log.debug(
                            "비활성 구역이라 데이터를 버립니다. deviceEui={}, zoneId={}",
                            deviceEui,
                            zone.zoneId()
                    );

                    return false;
                });
    }

    /**
     * 구역이 지금 운영 중인지 확인한다.
     * <p>
     * 인벤토리 장애일 때는 활성으로 본다. 조회가 안 된다고 멀쩡한 구역의 수집과 알림까지
     * 멈추는 편이 더 위험하기 때문이다. 반면 구역이 정말 없어졌다면 데이터를 보낼 곳이 없으므로 비활성으로 본다.
     */
    public boolean isZoneActive(Long zoneId) {
        try {
            return cachedZoneLookup.findActivation(zoneId)
                    .map(ZoneActivationResponse::active)
                    .orElseGet(() -> {
                        log.warn("존재하지 않는 구역이라 비활성으로 처리합니다. zoneId={}", zoneId);
                        return false;
                    });

        } catch (ApiException e) {
            log.warn(
                    "구역 활성 여부 조회에 실패해 활성으로 처리합니다. zoneId={}, errorCode={}, reason={}",
                    zoneId,
                    e.getErrorCode(),
                    e.getMessage()
            );

            return true;
        }
    }
}
