package com.nhnacademy.ruleengine.engine.notification.impl;

import com.nhnacademy.ruleengine.engine.dto.notification.*;
import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
import com.nhnacademy.ruleengine.global.config.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class TelegramNotificationSender implements NotificationSender {

    private final RestClient restClient;
    private final TelegramProperties properties;

    public TelegramNotificationSender(
            @Qualifier("telegramRestClient") RestClient restClient,
            TelegramProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.TELEGRAM;
    }

    @Override
    public void send(NotificationRequest request, NotificationPreference preference) {
        if(preference.recipient()==null || preference.recipient().isBlank()){
            log.info("Telegram recipient가 없어 알림을 건너뜁니다. userId={}", preference.userId());
            return;
        }

        String text = request.title() + "\n\n" + request.content();

        try {
            TelegramApiResponse response = restClient.post()
                    .uri("/bot{token}/sendMessage", properties.botToken())
                    .body(new TelegramSendMessageRequest(preference.recipient(), text))
                    .retrieve()
                    .body(TelegramApiResponse.class);

            if (response == null || !response.ok()) {
                log.info("Telegram 알림 발송 실패. userId={}, chatId={}, description={}",
                        preference.userId(),
                        preference.recipient(),
                        response == null ? null : response.description()
                );
                return;
            }

            log.info("Telegram 알림 발송 성공. userId={}, chatId={}, title={}",
                    preference.userId(),
                    preference.recipient(),
                    request.title()
            );
        } catch (RestClientException e){
            log.warn("Telegram 알림 발송 중 예외 발생. userId={}, chatId={}",
                    preference.userId(),
                    preference.recipient()
            );
        }
    }
}


