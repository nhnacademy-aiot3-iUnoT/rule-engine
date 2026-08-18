//package com.nhnacademy.ruleengine.engine.notification.impl;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
//import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
//import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
//import com.nhnacademy.ruleengine.engine.dto.notification.KakaoMessageTemplate;
//import com.nhnacademy.ruleengine.engine.dto.notification.KakaoApiResponse;
//import com.nhnacademy.ruleengine.engine.notification.NotificationSender;
//import com.nhnacademy.ruleengine.global.config.KakaoTalkProperties;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.client.RestClientException;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
//@Component
//@Slf4j
//public class KakaoNotificationSender implements NotificationSender {
//    private final RestClient restClient;
//    private final ObjectMapper objectMapper;
//    private final KakaoTalkProperties properties;
//
//    public KakaoNotificationSender(@Qualifier("kakaoRestClient") RestClient restClient,
//                                   KakaoTalkProperties properties,
//                                   ObjectMapper objectMapper) {
//        this.restClient = restClient;
//        this.properties = properties;
//        this.objectMapper = objectMapper;
//    }
//
//    @Override
//    public NotificationChannel channel() {
//        return NotificationChannel.KAKAO;
//    }
//
//    @Override
//    public void send(NotificationRequest request, NotificationPreference preference) {
//        if(properties.accessToken() == null || properties.accessToken().isBlank()){
//            log.info(" accessToken가 없어 카카오톡 알림을 건너뜁니다. userId={}", preference.userId());
//            return;
//        }
//
//        String text = request.title() + "\n\n" + request.content();
//
//        try{
//            String templateObject = objectMapper.writeValueAsString(
//                    new KakaoMessageTemplate("text",
//                            text,
//                            new KakaoMessageTemplate.KakaoLink(
//                            "https://iunot.cloud/",
//                            "https://iunot.cloud/"),
//                            "확인")
//            );
//
//            KakaoApiResponse response = restClient.post()
//                    .uri("/v2/api/talk/memo/default/send")
//                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.accessToken())
//                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
//                    .body("template_object=" + URLEncoder.encode(templateObject, StandardCharsets.UTF_8))
//                    .retrieve()
//                    .body(KakaoApiResponse.class);
//
//            if(response == null || !Integer.valueOf(0).equals(response.resultCode())) {
//                log.warn("카카오톡 알림 전송 실패. userId={}, resultCode={}",
//                        preference.userId(),
//                        response == null ? null : response.resultCode());
//                return;
//            }
//
//            log.info("카카오톡 알림 발송 성공. userId={}, title={}",
//                    preference.userId(),
//                    request.title());
//
//        } catch (JsonProcessingException e) {
//            log.warn("카카오톡 메시지 템플릿 생성 실패. userId={}",
//                    preference.userId(),
//                    e);
//
//        } catch (RestClientException e) {
//            log.warn("카카오톡 알림 발송 중 예외 발생. userId={}",
//                    preference.userId(),
//                    e);
//        }
//
//    }
//}
