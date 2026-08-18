package com.nhnacademy.ruleengine.engine.node.impl;


import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.FlowProcessingCompletion;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
import com.nhnacademy.ruleengine.engine.service.NotificationPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationDispatchNodeTest {

    @Mock
    NotificationPreferenceService preferenceService;

    @Mock
    NotificationSender telegramSender;

    @Mock
    NotificationSender kakaoNotificationSender;

    NotificationDispatchNode node;
    FlowProcessingCompletion completion;

    @BeforeEach
    void setUp(){
        node = new NotificationDispatchNode(
                "notification-dispatch",
                preferenceService,
                List.of(telegramSender, kakaoNotificationSender)
        );

        completion = new FlowProcessingCompletion();
    }

    @Test
    void environmentStatusEventDto가_없으면_알림_발송_안함() throws Exception {
        Message message = new Message(Map.of(), completion);

        node.process(message);
        verifyNoInteractions(preferenceService, telegramSender, kakaoNotificationSender);
        completion.await(Duration.ofMillis(100));
    }

    @Test
    void preferences가_비었으면_알림_발송_안함() throws Exception {
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of());
        node.process(message);
        verify(preferenceService).findPreferences(1L, 2L, 3L);
        verifyNoInteractions(telegramSender, kakaoNotificationSender);
        completion.await(Duration.ofMillis(100));
    }

    @Test
    void 이벤트_상태가_WARNING이면_preference가_있어도_발송_안함() throws Exception {
        EnvironmentStatusEventDto event = event(EnvStatus.WARNING);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of(new NotificationPreference(
                        1L,
                        1L,
                        2L,
                        3L,
                        NotificationChannel.TELEGRAM,
                        true,
                        "7501086554"))
                );

        node.process(message);
        verify(preferenceService).findPreferences(1L, 2L, 3L);
        verify(telegramSender, never()).send(any(), any());
        verify(kakaoNotificationSender, never()).send(any(), any());
    }

    @Test
    void enabled가_False면_preference는_발송하지_않음() throws Exception{
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of(new NotificationPreference(
                        1L,
                        1L,
                        2L,
                        3L,
                        NotificationChannel.TELEGRAM,
                        false,
                        "7501086554"))
                );

        node.process(message);
        verify(preferenceService).findPreferences(1L, 2L, 3L);
        verifyNoInteractions(telegramSender);
        completion.await(Duration.ofMillis(100));
    }

    @Test
    void enabled가_True면_해당_sender만_호출() {
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of(
                        new NotificationPreference(
                        1L,
                        1L,
                        2L,
                        3L,
                        NotificationChannel.TELEGRAM,
                        true,
                        "7501086554"),

                        new NotificationPreference(
                                1L,
                                1L,
                                2L,
                                3L,
                                NotificationChannel.KAKAO,
                                false,
                                "test-kakao-receiver-id")
                        )
                );
        when(telegramSender.channel()).thenReturn(NotificationChannel.TELEGRAM);

        node.process(message);
        verify(preferenceService).findPreferences(1L, 2L, 3L);
        verify(telegramSender).send(any(NotificationRequest.class), any(NotificationPreference.class));
        verify(kakaoNotificationSender, never()).send(any(), any());
    }

    @Test
    void sender가_없는_채널이면_예외_없이_skip(){
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of(
                                new NotificationPreference(
                                        1L,
                                        1L,
                                        2L,
                                        3L,
                                        NotificationChannel.WEB,
                                        true,
                                        "7501086554")
                ));
        when(telegramSender.channel()).thenReturn(NotificationChannel.TELEGRAM);
        when(kakaoNotificationSender.channel()).thenReturn(NotificationChannel.KAKAO);

        assertDoesNotThrow(()-> node.process(message));

        verify(preferenceService).findPreferences(1L, 2L, 3L);
        verify(telegramSender, never()).send(any(), any());
        verify(kakaoNotificationSender, never()).send(any(), any());
    }

    @Test
    void 여러_preference_중_활성화되고_sender_매칭되는_것만_발송() {
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of(
                                new NotificationPreference(
                                        1L,
                                        1L,
                                        2L,
                                        3L,
                                        NotificationChannel.TELEGRAM,
                                        true,
                                        "7501086554"),

                                new NotificationPreference(
                                        1L,
                                        1L,
                                        2L,
                                        3L,
                                        NotificationChannel.KAKAO,
                                        false,
                                        "test-kakao-receiver-id")
                        )
                );

        when(telegramSender.channel()).thenReturn(NotificationChannel.TELEGRAM);

        node.process(message);
        verify(preferenceService).findPreferences(1L, 2L, 3L);
        verify(telegramSender).send(any(), any());
        verify(kakaoNotificationSender, never()).send(any(), any());
    }

    @Test
    void send가_예외를_던져도_다음_preference_처리_계속() {
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);


        NotificationPreference telegramPreference =  new NotificationPreference(
                                        1L,
                                        1L,
                                        2L,
                                        3L,
                                        NotificationChannel.TELEGRAM,
                                        true,
                                        "7501086554");

        NotificationPreference kakaoPreference = new NotificationPreference(
                                        1L,
                                        1L,
                                        2L,
                                        3L,
                                        NotificationChannel.KAKAO,
                                        true,
                                        "test-kakao-receiver-id");

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of(telegramPreference, kakaoPreference));

        when(telegramSender.channel()).thenReturn(NotificationChannel.TELEGRAM);
        when(kakaoNotificationSender.channel()).thenReturn(NotificationChannel.KAKAO);

        doThrow(new RuntimeException("telegram fail")).when(telegramSender).send(any(NotificationRequest.class), eq(telegramPreference));

        assertDoesNotThrow(() -> node.process(message));
        verify(kakaoNotificationSender).send(any(NotificationRequest.class), eq(kakaoPreference));
        verify(telegramSender).send(any(NotificationRequest.class), eq(telegramPreference));
    }

    @Test
    void 이벤트의_organization_storage_zone_id로_preference를_조회한다(){
        EnvironmentStatusEventDto event = event(EnvStatus.CRITICAL);
        Message message = message(event);

        when(preferenceService.findPreferences(1L, 2L, 3L))
                .thenReturn(List.of());

        node.process(message);
        verify(preferenceService).findPreferences(1l, 2L, 3L);
    }


    private Message message(EnvironmentStatusEventDto event) {
        return new Message(Map.of(MessageFields.ENVIRONMENT_STATUS_EVENT, event), completion);
    }

    private EnvironmentStatusEventDto event(EnvStatus currentStatus) {
        return new EnvironmentStatusEventDto(
                1L,
                "device-1",
                2L,
                3L,
                "temperature",
                ViolationType.ABOVE_MAX,
                EnvStatus.NORMAL,
                currentStatus,
                EnvironmentEventReason.STATUS_CHANGED,
                35.0,
                10.0,
                30.0,
                "C",
                "2026-08-12T10:00:00",
                "온도 초과"
        );
    }
}