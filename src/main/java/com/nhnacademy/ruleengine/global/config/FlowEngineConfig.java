package com.nhnacademy.ruleengine.global.config;

import com.nhnacademy.ruleengine.engine.FlowEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// FlowEngine을 Spring Bean으로 등록하고 종료 생명주기를 관리한다.
public class FlowEngineConfig {

    @Bean(destroyMethod = "shutdown")
    public FlowEngine flowEngine() {
        return new FlowEngine();
    }

}
