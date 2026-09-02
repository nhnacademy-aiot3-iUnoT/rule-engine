package com.nhnacademy.ruleengine.engine.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;


@ExtendWith(RestDocumentationExtension.class)
abstract class RestDocsSupport {

    protected MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setUpRestDocs(RestDocumentationContextProvider restDocumentation) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(restDocumentation)
                        .operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint()))
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    /**
     * 검증된 JWT 인증이 있는 상태로 만든다. subject 는 계정 UUID 이다.
     */
    protected void authenticate(UUID accountUuid) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject(accountUuid.toString())
                .build();

        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);
        authentication.setAuthenticated(true);

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
