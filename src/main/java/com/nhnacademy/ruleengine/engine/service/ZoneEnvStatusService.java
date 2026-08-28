package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.global.client.InventoryClient;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentDecisionState;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorKeys;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// 구역 단위 환경 상태를 계산해 데이터베이스에 반영한다.
@Service
@RequiredArgsConstructor
public class ZoneEnvStatusService {

    // NORMAL < WARNING < CRITICAL 순으로 선언되어 있어 선언 순서를 심각도로 쓴다.
    private static final Comparator<EnvStatus> BY_SEVERITY = Comparator.comparingInt(EnvStatus::ordinal);

    private final Map<Long, EnvStatus> lastReportedStatuses = new ConcurrentHashMap<>();

    private final EnvironmentDecisionStateRedisRepository decisionStateRepository;
    private final InventoryClient inventoryClient;

    // 상태가 직전에 보낸 값과 같으면 보내지 않고 빈 값을 돌려준다.
    public Optional<EnvStatus> reportZoneStatus(
            Long organizationId,
            Long storageId,
            Long zoneId
    ) {
        String zoneKey = SensorKeys.zoneOf(organizationId, storageId, zoneId);

        // 상태 판단 노드가 센서 상태를 이미 저장했으므로, 여기서는 읽어서 가장 나쁜 값만 고른다.
        EnvStatus zoneStatus = worstOf(decisionStateRepository.findSensorStatesByZone(zoneKey));

        if (zoneStatus == lastReportedStatuses.get(zoneId)) {
            return Optional.empty();
        }

        inventoryClient.updateEnvStatus(zoneId, zoneStatus);

        // 전송에 성공한 뒤에 기억한다. 실패하면 기록이 남지 않아 다음 이벤트에서 다시 시도된다.
        lastReportedStatuses.put(zoneId, zoneStatus);

        return Optional.of(zoneStatus);
    }

    public void resolveCriticalStates(
            Long organizationId,
            Long storageId,
            Long zoneId
    ) {
        String zoneKey = SensorKeys.zoneOf(organizationId, storageId, zoneId);

        decisionStateRepository.findSensorStatesByZone(zoneKey).forEach((sensorField, state) -> {
            if (state.state() != EnvStatus.CRITICAL) {
                return;
            }

            decisionStateRepository.save(
                    zoneKey,
                    sensorField,
                    new EnvironmentDecisionState(
                            EnvStatus.NORMAL,
                            null,
                            null,
                            null,
                            state.lastMeasuredAt()
                    )
            );
        });

        reportZoneStatus(organizationId, storageId, zoneId);
    }
    private EnvStatus worstOf(Map<String, EnvironmentDecisionState> states) {
        return states.values().stream()
                .map(EnvironmentDecisionState::state)
                .filter(Objects::nonNull)
                .max(BY_SEVERITY)
                .orElse(EnvStatus.NORMAL);
    }
}
