package com.nhnacademy.ruleengine.global.bootstrap;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.engine.FlowEngine;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import com.nhnacademy.ruleengine.mqtt.flow.MqttRuleFlowFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
// 애플리케이션 시작 시 활성화된 MQTT Flow를 생성하고 실행한다.
public class RuleEngineStartupRunner implements CommandLineRunner {

    private final FlowEngine flowEngine;
    private final MqttRuleFlowFactory mqttRuleFlowFactory;

    @Override
    public void run(String... args) {
        try {
            Flow flow = mqttRuleFlowFactory.create();
            flowEngine.registerAndStart(flow);

            log.info(
                    "[RuleEngine] MQTT flow started. flowId={}",
                    flow.getId()
            );

        } catch (Exception e) {
            log.error("[RuleEngine] MQTT flow start failed", e);
        }
    }
}
