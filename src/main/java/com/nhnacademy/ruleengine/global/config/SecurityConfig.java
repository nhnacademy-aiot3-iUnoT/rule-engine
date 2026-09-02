package com.nhnacademy.ruleengine.global.config;


import com.nhnacademy.ruleengine.global.security.ApiAccessDeniedHandler;
import com.nhnacademy.ruleengine.global.security.ApiAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter converter,
            ApiAuthenticationEntryPoint entryPoint,
            ApiAccessDeniedHandler accessDeniedHandler
    ) throws Exception{
        http.csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                )

                .oauth2ResourceServer(resourceServer -> resourceServer
                        .authenticationEntryPoint(entryPoint)
                        .jwt(jwt -> jwt
                                .decoder(jwtDecoder)
                                .jwtAuthenticationConverter(converter)
                        )
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health", "/actuator/health/**", "/actuator/serviceregistry", "/actuator/prometheus"
                        ).permitAll()
                        .requestMatchers("/docs/**").permitAll()
                        .requestMatchers("/api/rule-engine/internal/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder(
            JwtProperties properties
    ) {
        Assert.hasText(properties.getIssuer(), "security.jwt.issuer must be configured");
        Assert.notEmpty(properties.getAudiences(), "security.jwt.audiences must not be empty");
        Assert.notEmpty(
                properties.getAllowedAlgorithms(),
                "security.jwt.allowed-algorithms must not be empty"
        );
        Assert.hasText(properties.getJwkSetUri(),  "security.jwt.jwk-set-uri must not be empty");

        NimbusJwtDecoder.JwkSetUriJwtDecoderBuilder builder =
                NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri());
        properties.getAllowedAlgorithms().forEach(builder::jwsAlgorithm);

        NimbusJwtDecoder decoder = builder.build();
        decoder.setJwtValidator(jwtValidator(properties));

        return decoder;
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setPrincipalClaimName("sub");
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);

        return converter;
    }

    private OAuth2TokenValidator<Jwt> jwtValidator(JwtProperties properties) {
        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(30));

        timestampValidator.setAllowEmptyExpiryClaim(false);

        List<OAuth2TokenValidator<Jwt>> validators = new ArrayList<>();
        validators.add(JwtValidators.createDefaultWithValidators(
                timestampValidator,
                new JwtIssuerValidator(properties.getIssuer())
        ));

        validators.add(jwt -> jwt.getHeaders().get("kid") instanceof String kid && !kid.isBlank()
                ? OAuth2TokenValidatorResult.success()
                : validationFailure("JWT kid header is required"));


        validators.add(this::validateUuidSubject);

        validators.add(jwt -> jwt.getAudience().stream()
                .anyMatch(properties.getAudiences()::contains)
                ? OAuth2TokenValidatorResult.success()
                : validationFailure("JWT audience is not allowed"));

        return new DelegatingOAuth2TokenValidator<>(validators);
    }



    private OAuth2TokenValidatorResult validateUuidSubject(Jwt jwt) {
        try {
            UUID.fromString(jwt.getSubject());
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException | NullPointerException exception) {
            return validationFailure("JWT subject must be an account UUID");
        }
    }

    private OAuth2TokenValidatorResult validationFailure(String description) {
        return OAuth2TokenValidatorResult.failure(
                new OAuth2Error("invalid_token", description, null)
        );
    }
}
