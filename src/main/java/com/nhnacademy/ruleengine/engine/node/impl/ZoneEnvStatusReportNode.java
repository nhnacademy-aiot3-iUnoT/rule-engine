package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.ZoneEnvStatusService;
import lombok.extern.slf4j.Slf4j;

// 상태 이벤트가 나온 구역의 환경 상태를 인벤토리에 반영한다.
// 계산과 전송은 ZoneEnvStatusService가 하고, 이 노드는 파이프라인 배선만 담당한다.
// 알림 발송은 센서별 그대로 둔다(어느 센서가 문제인지 알아야 한다).
@Slf4j
public class ZoneEnvStatusReportNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final ZoneEnvStatusService zoneEnvStatusService;

    public ZoneEnvStatusReportNode(
            String id,
            ZoneEnvStatusService zoneEnvStatusService
    ) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        this.zoneEnvStatusService = zoneEnvStatusService;
    }

    @Override
    protected void onProcess(Message message) {
        EnvironmentStatusEventDto event = message.get(MessageFields.ENVIRONMENT_STATUS_EVENT);

        // 상위 노드가 이벤트를 채워 보내므로 여기 걸리면 배선이나 payload 계약이 깨진 것이다.
        if (event == null) {
            log.error("[{}] event가 없습니다. 상위 노드의 payload 계약이 깨졌습니다.", getId());
            return;
        }

        // 구역 상태 반영에 실패하더라도 알림 발송은 계속되어야 한다.
        try {
            // 직전에 보낸 값과 같으면 전송하지 않으므로 빈 값이 올 수 있다.
            zoneEnvStatusService.reportZoneStatus(
                    event.organizationId(),
                    event.storageId(),
                    event.zoneId()
            ).ifPresent(zoneStatus -> log.info(
                    "[{}] 구역 환경 상태를 갱신했습니다. zoneId={}, status={}, 이벤트 센서={}/{}",
                    getId(), event.zoneId(), zoneStatus, event.deviceEui(), event.sensorType()
            ));

        } catch (Exception e) {
            log.warn("[{}] 구역 환경 상태 반영에 실패했습니다. zoneId={}", getId(), event.zoneId(), e);
        }

        send(OUTPUT_PORT, message);
    }
}
