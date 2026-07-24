package com.nhnacademy.ruleengine.global.bootstrap;

import com.nhnacademy.ruleengine.engine.Flow;
import com.nhnacademy.ruleengine.engine.FlowEngine;
import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog;
import com.nhnacademy.ruleengine.engine.flow.FlowFactory;

import com.nhnacademy.ruleengine.engine.service.SectionEnvironmentFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
// 애플리케이션 시작 시 활성화된 MQTT Flow를 생성하고 실행한다.
public class RuleEngineStartupRunner implements CommandLineRunner {

    private final FlowEngine flowEngine;
    private final List<FlowFactory> flowFactories;
    private final SectionCatalog sectionCatalog;
    private final SectionEnvironmentFlowService sectionEnvironmentFlowService;

    @Override
    public void run(String... args) {
        for (FlowFactory flowFactory : flowFactories) {
            try {
                start(flowFactory.create());
            } catch (Exception e) {
                log.error(
                        "[RuleEngine] flow creation failed. factory={}",
                        flowFactory.getClass().getSimpleName(),
                        e
                );
            }
        }

            //섹션아이디목록을 조회여하여 실행
            List<Long> sectionIds = sectionCatalog.findAllSectionIds();
            for(Long sectionId : sectionIds){
                try{
                    sectionEnvironmentFlowService.createAndStart(sectionId);
                } catch (Exception e) {
                    log.error("[RuleEngine] section environment flow creation failed. sectionId={}", sectionId, e);
                }
            }
    }

    private void start(Flow flow) {
        try {
            flowEngine.registerAndStart(flow);
            log.info("[RuleEngine] flow started. flowId={}", flow.getId());
        } catch (Exception e) {
            log.error(
                    "[RuleEngine] flow start failed. flowId={}",
                    flow.getId(),
                    e
            );
        }
    }
}
