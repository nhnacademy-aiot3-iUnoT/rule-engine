package com.nhnacademy.ruleengine.engine.flow;

import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.node.MqttNodeConfigFactory;
import com.nhnacademy.ruleengine.engine.service.SensorInfluxService;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import com.nhnacademy.ruleengine.global.config.RuleEngineProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SectionEnvironmentFlowFactory {

    private final RuleEngineProperties properties;
    private final MqttNodeConfigFactory mqttNodeConfigFactory;
    private final SensorInfluxService sensorInfluxService;
    private final ThresholdPolicyService thresholdPolicyService;


    public Flow create(Long sectionId){
        log.info("[{} flow 생성 ]",sectionId);
        return new SectionEnvironmentFlow(
                sectionId,
                properties,
                mqttNodeConfigFactory,
                sensorInfluxService,
                thresholdPolicyService
        ).create();
    }

    public List<Flow> create(List<Long> sectionIds){
        return sectionIds.stream()
                .map(this::create)
                .toList();

    }


}
