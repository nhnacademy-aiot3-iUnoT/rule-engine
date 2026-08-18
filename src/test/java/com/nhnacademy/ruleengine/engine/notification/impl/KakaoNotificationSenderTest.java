//package com.nhnacademy.ruleengine.engine.notification.impl;
//
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
//import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentEventReason;
//import com.nhnacademy.ruleengine.engine.dto.notification.NotificationChannel;
//import com.nhnacademy.ruleengine.engine.dto.notification.NotificationPreference;
//import com.nhnacademy.ruleengine.engine.dto.notification.NotificationRequest;
//import com.nhnacademy.ruleengine.global.config.KakaoTalkProperties;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.client.MockRestServiceServer;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.client.RestClientException;
//
//import java.io.IOException;
//import java.util.Map;
//
//import static org.hamcrest.Matchers.startsWith;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
//import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
//
//@ExtendWith(MockitoExtension.class)
//class KakaoNotificationSenderTest {
//
//    RestClient.Builder builder;
//
//    MockRestServiceServer server;
//
//    RestClient restClient;
//
//    KakaoNotificationSender kakaoNotificationSender;
//
//
//    ObjectMapper objectMapper;
//
//
//    @BeforeEach
//    void setUp(){
//        builder = RestClient.builder()
//                .baseUrl("https://kapi.kakao.com");
//
//        server = MockRestServiceServer.bindTo(builder).build();
//        restClient = builder.build();
//
//        objectMapper = new ObjectMapper();
//
//        kakaoNotificationSender =  new KakaoNotificationSender(restClient, propertiesWithAccessToken(), objectMapper);
//    }
//
//    @Test
//    void channel은_NotificationChannel_KAKAO_반환() {
//        assertEquals(NotificationChannel.KAKAO, kakaoNotificationSender.channel());
//    }
//
//    @Test
//    void accessToken이_null이면_발송_skip() throws Exception {
//        objectMapper = mock(ObjectMapper.class);
//
//        kakaoNotificationSender = new KakaoNotificationSender(restClient, propertiesWithoutAccessToken(), objectMapper);
//
//        kakaoNotificationSender.send(request(), preference());
//
//        verify(objectMapper, never()).writeValueAsString(any());
//    }
//
//    @Test
//    void accessToken이_blank면_발송_skip() throws Exception {
//        objectMapper = mock(ObjectMapper.class);
//
//        KakaoTalkProperties properties =
//                new KakaoTalkProperties("client-id", "https://kapi.kakao.com", " ");
//
//        kakaoNotificationSender = new KakaoNotificationSender(restClient, properties, objectMapper);
//
//        kakaoNotificationSender.send(request(), preference());
//
//        verify(objectMapper, never()).writeValueAsString(any());
//    }
//
//    @Test
//    void 정상_응답_resultCode가_0이면_예외_없이_성공() {
//        server.expect(requestTo("https://kapi.kakao.com/v2/api/talk/memo/default/send"))
//                .andExpect(method(HttpMethod.POST))
//                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
//                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
//                .andExpect(content().string(startsWith("template_object=")))
//                .andRespond(withSuccess("""
//                        {"result_code":0}""",MediaType.APPLICATION_JSON));
//
//        kakaoNotificationSender.send(request(), preference());
//
//        server.verify();
//    }
//
//    @Test
//    void 응답이_null이면_예외_없이_실패_처리() {
//        server.expect(requestTo("https://kapi.kakao.com/v2/api/talk/memo/default/send"))
//                .andExpect(method(HttpMethod.POST))
//                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
//                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
//                .andExpect(content().string(startsWith("template_object=")))
//                .andRespond(withSuccess("",MediaType.APPLICATION_JSON));
//
//        assertDoesNotThrow(()-> kakaoNotificationSender.send(request(), preference()));
//
//        server.verify();
//    }
//
//    @Test
//    void 응답_resultCode가_0이_아니면_예외_없이_실패_처리() {
//        server.expect(requestTo("https://kapi.kakao.com/v2/api/talk/memo/default/send"))
//                .andExpect(method(HttpMethod.POST))
//                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
//                .andExpect(header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE))
//                .andExpect(content().string(startsWith("template_object=")))
//                .andRespond(withSuccess("""
//                        {"result_code":2}""",MediaType.APPLICATION_JSON));
//
//        assertDoesNotThrow(() -> kakaoNotificationSender.send(request(), preference()));
//
//        server.verify();
//    }
//
//    @Test
//    void ObjectMapper_writeValueAsString에서_예외_발생하면_잡음() throws Exception {
//        restClient = mock(RestClient.class);
//        objectMapper = mock(ObjectMapper.class);
//        kakaoNotificationSender = new KakaoNotificationSender(restClient, propertiesWithAccessToken(), objectMapper);
//
//        when(objectMapper.writeValueAsString(any())).thenThrow(JsonProcessingException.class);
//
//        assertDoesNotThrow(() -> kakaoNotificationSender.send(request(), preference()));
//
//        verify(objectMapper).writeValueAsString(any());
//        verifyNoInteractions(restClient);
//    }
//
//    @Test
//    void RestClient_호출_중_예외_발생하면_잡음() {
//        restClient = mock(RestClient.class);
//
//        kakaoNotificationSender = new KakaoNotificationSender(restClient, propertiesWithAccessToken(), objectMapper);
//
//        when(restClient.post()).thenThrow(new RestClientException("fail"));
//
//        assertDoesNotThrow(() -> kakaoNotificationSender.send(request(), preference()));
//
//        verify(restClient).post();
//    }
//
//    private KakaoTalkProperties propertiesWithAccessToken() {
//        return new KakaoTalkProperties("client-id", "https://kapi.kakao.com", "access-token");
//    }
//    private KakaoTalkProperties propertiesWithoutAccessToken(){
//        return new KakaoTalkProperties("client-id", "https://kapi.kakao.com", null);
//    }
//
//
//    private NotificationRequest request() {
//        return new NotificationRequest(
//                1L,
//                2L,
//                3L,
//                "device-1",
//                "temperature",
//                EnvStatus.NORMAL,
//                EnvStatus.CRITICAL,
//                EnvironmentEventReason.STATUS_CHANGED,
//                "온도 이상",
//                "현재 온도가 기준치를 초과했습니다.",
//                "2026-08-12T10:00:00",
//                Map.of()
//        );
//    }
//
//    private NotificationPreference preference(){
//        return new NotificationPreference(
//                1L,
//                1L,
//                2L,
//                3L,
//                NotificationChannel.KAKAO,
//                true,
//                "test-kakao-receiver-id");
//    }
//
//
//
//
//
//}