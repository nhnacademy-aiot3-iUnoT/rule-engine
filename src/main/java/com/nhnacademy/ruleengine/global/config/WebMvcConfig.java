package com.nhnacademy.ruleengine.global.config;

import com.nhnacademy.ruleengine.global.security.RuleEngineUuidArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RuleEngineUuidArgumentResolver ruleEngineUuidArgumentResolver;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(ruleEngineUuidArgumentResolver);
    }
}
