package com.nhnacademy.ruleengine.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String DEVICE_ZONE = "device-zone";

    public static final String ZONE_THRESHOLD = "zone-threshold";

    public static final String MEMBER_ORGANIZATION = "member-organization";

    public static final String ZONE_LOCATION = "zone-location";

    private static final Duration DEVICE_ZONE_TTL = Duration.ofMinutes(30);

    private static final Duration DEVICE_ZONE_MISS_TTL = Duration.ofMinutes(2);

    private static final Duration ZONE_THRESHOLD_TTL = Duration.ofMinutes(1);

    // 조직 탈퇴/역할 변경이 곧바로 반영되도록 짧게 잡는다.
    private static final Duration MEMBER_ORGANIZATION_TTL = Duration.ofMinutes(1);

    // 구역이 속한 조직/창고는 거의 바뀌지 않아 길게 잡는다.
    private static final Duration ZONE_LOCATION_TTL = Duration.ofMinutes(30);

    // 없는 구역은 곧 생길 수 있으니 짧게 잡는다.
    private static final Duration ZONE_LOCATION_MISS_TTL = Duration.ofMinutes(2);

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.setCacheNames(List.of());

        cacheManager.registerCustomCache(
                DEVICE_ZONE,
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfter(Expiry.creating(CacheConfig::deviceZoneTtl))
                        .build()
        );

        cacheManager.registerCustomCache(
                ZONE_THRESHOLD,
                Caffeine.newBuilder()
                        .maximumSize(2_000)
                        .expireAfterWrite(ZONE_THRESHOLD_TTL)
                        .build()
        );

        cacheManager.registerCustomCache(
                MEMBER_ORGANIZATION,
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfterWrite(MEMBER_ORGANIZATION_TTL)
                        .build()
        );

        cacheManager.registerCustomCache(
                ZONE_LOCATION,
                Caffeine.newBuilder()
                        .maximumSize(10_000)
                        .expireAfter(Expiry.creating(CacheConfig::zoneLocationTtl))
                        .build()
        );

        return cacheManager;
    }

    // 조회 결과가 없으면 Spring이 Optional을 벗겨 NullValue로 저장하므로, 값 타입으로 성공/미등록을 구분한다.
    private static Duration deviceZoneTtl(Object key, Object value) {
        return value instanceof ResolvedZoneResponse
                ? DEVICE_ZONE_TTL
                : DEVICE_ZONE_MISS_TTL;
    }

    // 없는 구역(NullValue)까지 오래 붙잡고 있지 않도록 성공/미등록을 나눈다.
    private static Duration zoneLocationTtl(Object key, Object value) {
        return value instanceof ResolvedZoneResponse
                ? ZONE_LOCATION_TTL
                : ZONE_LOCATION_MISS_TTL;
    }
}
