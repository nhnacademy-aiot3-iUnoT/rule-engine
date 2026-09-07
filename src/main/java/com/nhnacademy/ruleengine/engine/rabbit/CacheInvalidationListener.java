package com.nhnacademy.ruleengine.engine.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.ruleengine.global.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.nhnacademy.ruleengine.engine.repository.EnvironmentDecisionStateRedisRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheInvalidationListener {

    private static final Map<String, Function<String, Object>> KEY_PARSERS = Map.of(
            CacheConfig.DEVICE_ZONE, key -> key,
            CacheConfig.ZONE_LOCATION, Long::valueOf,
            CacheConfig.ZONE_ACTIVATION, Long::valueOf,
            CacheConfig.ZONE_THRESHOLD, Long::valueOf,
            CacheConfig.MEMBER_ORGANIZATION, UUID::fromString
    );

    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    private final EnvironmentDecisionStateRedisRepository decisionStateRepository;

    @RabbitListener(queues = "#{cacheInvalidationQueue.name}")
    public void onCacheInvalidation(String rawPayload) {
        CacheInvalidationMessage message = deserialize(rawPayload);
        if (message == null) {
            return;
        }

        // 환경상태 판단 상태는 Caffeine 캐시가 아니라 Redis에 있어서 따로 처리한다.
        if (CacheConfig.ZONE_DECISION_STATE.equals(message.cacheName())) {
            evictDecisionState(message);
            return;
        }

        Function<String, Object> keyParser = KEY_PARSERS.get(message.cacheName());
        if (keyParser == null) {
            log.warn("알 수 없는 캐시 이름입니다. cacheName={}", message.cacheName());
            return;
        }

        Cache cache = cacheManager.getCache(message.cacheName());
        if (cache == null) {
            log.warn("등록되지 않은 캐시입니다. cacheName={}", message.cacheName());
            return;
        }

        evict(cache, message, keyParser);
    }

    private void evictDecisionState(CacheInvalidationMessage message) {
        if (message.key() == null || message.key().isBlank()) {
            log.warn("판단 상태를 지울 구역 키가 없습니다.");
            return;
        }

        decisionStateRepository.deleteByZone(message.key());
        log.info("구역의 환경상태 판단 상태를 초기화했습니다. zoneKey={}", message.key());
    }

    private void evict(Cache cache, CacheInvalidationMessage message, Function<String, Object> keyParser) {
        if (message.key() == null) {
            cache.clear();
            log.debug("캐시를 전부 비웠습니다. cacheName={}", message.cacheName());
            return;
        }

        try {
            cache.evictIfPresent(keyParser.apply(message.key()));
            log.debug("캐시를 비웠습니다. cacheName={}, key={}", message.cacheName(), message.key());
        } catch (IllegalArgumentException exception) {
            log.error(
                    "캐시 키 형식이 맞지 않습니다. cacheName={}, key={}",
                    message.cacheName(),
                    message.key()
            );
        }
    }

    // 형식이 깨진 메시지는 다시 넣어봐야 똑같이 실패하므로 버린다.
    private CacheInvalidationMessage deserialize(String rawPayload) {
        try {
            return objectMapper.readValue(rawPayload, CacheInvalidationMessage.class);
        } catch (Exception exception) {
            log.error("캐시 무효화 메시지 역직렬화 실패. payload={}", rawPayload);
            return null;
        }
    }
}
