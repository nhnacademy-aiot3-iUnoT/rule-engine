package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
import com.nhnacademy.ruleengine.engine.service.NotificationPreferenceService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

// 정규화 센서 파이프라인의 종단 노드다. 출력 포트가 없으므로 여기서 처리가 끝난다.
@Slf4j
public class NotificationDispatchNode extends AbstractNode {

    private static final String INPUT_PORT = "in";

    private final NotificationPreferenceService notificationPreferenceService;
    private final List<NotificationSender> senders;


    public NotificationDispatchNode(
            String id,
            NotificationPreferenceService notificationPreferenceService,
            List<NotificationSender> senders
    ) {
        super(id);
        this.notificationPreferenceService = notificationPreferenceService;
        this.senders = senders;
        addInputPort(INPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        EnvironmentStatusEventDto event = message.get(MessageFields.ENVIRONMENT_STATUS_EVENT);

        if(event == null){
            log.error("[{}] event가 없습니다. 상위 노드의 payload 계약이 깨졌습니다.", getId());
            return;
        }

        NotificationRequest request = NotificationRequest.from(event);

        List<NotificationPreference> preferences = notificationPreferenceService.findPreferences(request.organizationId(), request.storageId(), request.sectionId());

        if (preferences==null || preferences.isEmpty()) {
            log.info(
                    "[{}] preferences(알림 설정)이 없어 발송을 건너뜁니다. organizationId={}, storageId={}, sectionId={}",
                    getId(),
                    request.organizationId(),
                    request.storageId(),
                    request.sectionId()
            );
            message.completeProcessing();
            return;
        }

        if(event.currentStatus()==EnvironmentStatus.WARNING){
            message.completeProcessing();
            return;
        }

        for(NotificationPreference preference : preferences){
            if(!preference.enabled()){
                continue;
            }

            try {
                senders.stream()
                        .filter(sender -> sender.channel().equals(preference.channel()))
                        .findFirst()
                        .ifPresentOrElse(
                                sender -> sender.send(request, preference),
                                () -> log.info(
                                        "[{}] 지원하지 않는 채널이라 알림 발송을 건너뜁니다. userId={}, channel={}",
                                        getId(),
                                        preference.userId(),
                                        preference.channel()
                                )
                        );
            } catch (Exception e) {
                log.warn("[{}] 알림 발송 중 예외가 발생했습니다. userId={}, channel={}",
                        getId(),
                        preference.userId(),
                        preference.channel(),
                        e
                );
            }
        }
    }
}
