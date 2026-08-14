package com.nhnacademy.ruleengine.engine.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.engine.exception.ApiException;
import com.nhnacademy.ruleengine.global.dto.ApiResponse;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.util.function.Supplier;

@Slf4j
@Component
public class ApiClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ApiClient(
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }


    public <T> T get(String url, Class<T> dataType) {
        return get(url, responseTypeOf(dataType));
    }


    public <T> T get(String url, ParameterizedTypeReference<ApiResponse<T>> responseType) {
        return execute(() ->
                restClient.get()
                        .uri(URI.create(url))
                        .retrieve()
                        .body(responseType));
    }

    public <T> T post(String url, Object body, Class<T> dataType) {
        return post(url, body, responseTypeOf(dataType));
    }

    public <T> T post(String url, Object body, ParameterizedTypeReference<ApiResponse<T>> responseType) {
        return execute(() ->
                restClient.post()
                        .uri(URI.create(url))
                        .body(body)
                        .retrieve()
                        .body(responseType));
    }

    public void post(String url, Object body) {
        executeBodiless(() ->
                restClient.post()
                        .uri(URI.create(url))
                        .body(body)
                        .retrieve()
                        .toBodilessEntity());
    }

    public <T> T put(String url, Object body, Class<T> dataType) {
        return put(url, body, responseTypeOf(dataType));
    }

    public <T> T put(String url, Object body, ParameterizedTypeReference<ApiResponse<T>> responseType) {
        return execute(() ->
                restClient.put()
                        .uri(URI.create(url))
                        .body(body)
                        .retrieve()
                        .body(responseType));
    }

    public void put(String url, Object body) {
        executeBodiless(() ->
                restClient.put()
                        .uri(URI.create(url))
                        .body(body)
                        .retrieve()
                        .toBodilessEntity());
    }

    public <T> T delete(String url, ParameterizedTypeReference<ApiResponse<T>> responseType) {
        return execute(() ->
                restClient.delete()
                        .uri(URI.create(url))
                        .retrieve()
                        .body(responseType));
    }

    public void delete(String url) {
        executeBodiless(() ->
                restClient.delete()
                        .uri(URI.create(url))
                        .retrieve()
                        .toBodilessEntity());
    }

    private <T> T execute(Supplier<ApiResponse<T>> supplier) {
        try {
            ApiResponse<T> response = supplier.get();

            if (response == null) {
                throw new ApiException(
                        ErrorCode.EXTERNAL_API_EMPTY_RESPONSE,
                        "응답이 없습니다."
                );
            }

            if (!response.success()) {
                throw new ApiException(
                        ErrorCode.from(response.error() == null ? null : response.error().code()),
                        response.error() == null ? "외부 서비스가 실패를 반환했습니다." : response.error().message()
                );
            }

            return response.data();

        } catch (HttpStatusCodeException e) {
            throw convertApiException(e); // API Server가 준 JSON -> 내부 예외 객체로 변환

        } catch (RestClientException e) {
            throw convertApiException(e); // 연결 실패, 타임아웃, 응답 파싱 실패 등
        }
    }

    private void executeBodiless(Runnable request) {
        try {
            request.run();

        } catch (HttpStatusCodeException e) {
            throw convertApiException(e);

        } catch (RestClientException e) {
            throw convertApiException(e);
        }
    }

    private ApiException convertApiException(HttpStatusCodeException e) {
        String responseBody = e.getResponseBodyAsString();

        log.error(
                "API request failed. status={}, body={}",
                e.getStatusCode(),
                responseBody
        );

        try {
            ApiResponse<Void> response = objectMapper.readValue(
                    responseBody,
                    new TypeReference<>() {}
            );

            return new ApiException(
                    ErrorCode.from(response.error().code()),
                    response.error().message()
            );

        } catch (Exception ex) {
            // 상대 서비스가 ApiResponse 형식이 아닌 본문을 준 경우
            return new ApiException(
                    ErrorCode.EXTERNAL_API_ERROR,
                    "서버 요청 처리 중 오류가 발생했습니다."
            );
        }
    }

    private ApiException convertApiException(RestClientException e) {
        log.error("API request failed. message={}", e.getMessage(), e);

        return new ApiException(
                ErrorCode.EXTERNAL_API_ERROR,
                "외부 서비스 호출에 실패했습니다."
        );
    }

    private <T> ParameterizedTypeReference<ApiResponse<T>> responseTypeOf(Class<T> dataType) {
        ResolvableType type = ResolvableType.forClassWithGenerics(ApiResponse.class, dataType);
        return ParameterizedTypeReference.forType(type.getType());
    }
}
