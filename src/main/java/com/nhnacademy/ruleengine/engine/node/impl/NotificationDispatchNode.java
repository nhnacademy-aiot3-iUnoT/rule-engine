package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.service.NotificationPreferenceService;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class NotificationDispatchNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final NotificationPreferenceService notificationPreferenceService;

    public NotificationDispatchNode(String id, NotificationPreferenceService notificationPreferenceService) {
        super(id);
        this.notificationPreferenceService = notificationPreferenceService;
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        EnvironmentStatusEventDto event = message.get(MessageFields.ENVIRONMENT_STATUS_EVENT);

        if(event == null){
            log.info("[{}] event가 없어 알림 발송을 건너뜁니다.", getId());
            message.completeProcessing();
            return;
        }

        NotificationRequest request = NotificationRequest.from(event);

        List<NotificationPreference> preferences = notificationPreferenceService.findPreferences(request.organizationId(), request.storageId(), request.sectionId());

        for(NotificationPreference preference : preferences){
            if(preference.enabled()){
                log.info(
                        "[{}] 알림 발송 시뮬레이션. userId={}, channel={}, recipient={}, title={}, content={}",
                        getId(),
                        preference.userId(),
                        preference.channel(),
                        preference.recipient(),
                        request.title(),
                        request.content()
                );
            }
        }

        send(OUTPUT_PORT, message);
    }
}
