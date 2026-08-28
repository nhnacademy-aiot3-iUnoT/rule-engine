package com.nhnacademy.ruleengine.engine.notification.impl;

import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
import com.nhnacademy.ruleengine.global.config.TelegramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TelegramNotificationSenderTest {

    RestClient.Builder builder;

    MockRestServiceServer server;

    RestClient restClient;

    TelegramNotificationSender telegramNotificationSender;

    @BeforeEach
    void setUp(){
        builder = RestClient.builder()
                .baseUrl("https://api.telegram.org");

        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();

        telegramNotificationSender =  new TelegramNotificationSender(restClient, properties());
    }

    @Test
    void channel은_NotificationChannel_KAKAO_반환() {
        assertEquals(NotificationChannel.TELEGRAM, telegramNotificationSender.channel());
    }

    @Test
    void preference_recipient가_null이면_발송_skip() {
        restClient = mock(RestClient.class);
        telegramNotificationSender = new TelegramNotificationSender(restClient, properties());

        NotificationPreference preference= new NotificationPreference(
                1L,
                1L,
                2L,
                3L,
                NotificationChannel.TELEGRAM,
                true, null);

        telegramNotificationSender.send(request(), preference);

        verify(restClient, never()).post();
    }

    @Test
    void preference_recipient가_blank이면_발송_skip() {
        restClient = mock(RestClient.class);
        telegramNotificationSender = new TelegramNotificationSender(restClient, properties());

        NotificationPreference preference= new NotificationPreference(
                1L,
                1L,
                2L,
                3L,
                NotificationChannel.TELEGRAM,
                true, " ");

        telegramNotificationSender.send(request(), preference);

        verify(restClient, never()).post();
    }

    @Test
    void 정상_응답_ok_true이면_텔레그램_API를_호출한다() {
        server.expect(requestTo("https://api.telegram.org/botbotToken/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"chat_id\":\"7501086554\"")))
                .andExpect(content().string(containsString("\"text\":\"온도 이상\\n\\n현재 온도가 기준치를 초과했습니다.\"")))
                .andRespond(withSuccess("""
                        {"ok":true}
                        """, MediaType.APPLICATION_JSON));

        telegramNotificationSender.send(request(), preference());

        server.verify();
    }

    @Test
    void 응답_null이어도_예외_없이_실패_처리한다() {
        server.expect(requestTo("https://api.telegram.org/botbotToken/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          ""
                        }
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() ->
                telegramNotificationSender.send(request(), preference())
        );

        server.verify();
    }

    @Test
    void 응답_ok_false이면_예외_없이_실패_처리한다() {
        server.expect(requestTo("https://api.telegram.org/botbotToken/sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "ok": false,
                          "error_code": 400,
                          "description": "Bad Request"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() ->
                telegramNotificationSender.send(request(), preference())
        );

        server.verify();
    }

    @Test
    void RestClient_호출_중_예외가_발생하면_던지지않고_잡는다() {
        RestClient mockRestClient = mock(RestClient.class);

        TelegramNotificationSender sender = new TelegramNotificationSender(
                mockRestClient,
                properties()
        );

        when(mockRestClient.post()).thenThrow(new RestClientException("telegram fail"));

        assertDoesNotThrow(() ->
                sender.send(request(), preference())
        );

        verify(mockRestClient).post();
    }





    private TelegramProperties properties() {
        return new TelegramProperties("botToken", "https://api.telegram.org");
    }

    private NotificationRequest request() {
        return new NotificationRequest(
                1L,
                2L,
                3L,
                "device-1",
                "temperature",
                EnvStatus.NORMAL,
                EnvStatus.CRITICAL,
                EnvironmentEventReason.STATUS_CHANGED,
                ViolationType.ABOVE_MAX,
                30.0,
                18.0,
                26.0,
                "온도 이상",
                "현재 온도가 기준치를 초과했습니다.",
                "2026-08-12T10:00:00",
                Map.of()
        );
    }

    private NotificationPreference preference(){
        return new NotificationPreference(
                1L,
                1L,
                2L,
                3L,
                NotificationChannel.TELEGRAM,
                true,
                "7501086554");
    }

}