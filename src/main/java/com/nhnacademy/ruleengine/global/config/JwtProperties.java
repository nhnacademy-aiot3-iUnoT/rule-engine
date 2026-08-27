package com.nhnacademy.ruleengine.global.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Validated
@ConfigurationProperties("security.jwt")
public class JwtProperties {
    @Setter
    @NotBlank
    private String issuer;

    @Setter
    private Set<String> audiences;

    @Setter
    @NotNull
    private Duration accessTokenTtl;

    @Setter
    @NotBlank
    private String keyId;

    @Setter
    @NotBlank
    private String jwkSetUri;


    @Setter
    private Set<SignatureAlgorithm> allowedAlgorithms =
            new LinkedHashSet<>(Set.of(SignatureAlgorithm.RS256));

    @AssertTrue(message = "security.jwt.access-token-ttl must be positive")
    public boolean isAccessTokenTtlPositive() {
        return accessTokenTtl == null || accessTokenTtl.compareTo(Duration.ZERO) > 0;
    }

}