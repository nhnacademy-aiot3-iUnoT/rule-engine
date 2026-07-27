package com.nhnacademy.ruleengine.engine.service;


import com.nhnacademy.ruleengine.engine.core.Flow;
import com.nhnacademy.ruleengine.engine.core.FlowEngine;
import com.nhnacademy.ruleengine.engine.flow.SectionEnvironmentFlow;
import com.nhnacademy.ruleengine.engine.flow.SectionEnvironmentFlowFactory;
import com.nhnacademy.ruleengine.engine.validation.LocationHierarchyValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SectionEnvironmentFlowService {

    private final FlowEngine flowEngine;
    private final SectionEnvironmentFlowFactory sectionEnvironmentFlowFactory;
    private final LocationHierarchyValidator locationHierarchyValidator;

    public void createAndStart(Long organizationId, Long storageId, Long sectionId){

        locationHierarchyValidator.validateSection(organizationId, storageId, sectionId);
        String flowId = SectionEnvironmentFlow.flowId(sectionId);

        if(flowEngine.getFlows().containsKey(flowId)){
            log.info("flowId={}는 이미 생성된 플로우입니다.", flowId);
            return;
        }

        Flow sectionEnvironmentFlow = sectionEnvironmentFlowFactory.create(sectionId);
        flowEngine.registerAndStart(sectionEnvironmentFlow);
        log.info("플로우를 생성하고 시작했습니다. flowId={}", flowId);
    }

    public void createAndStart(Long sectionId){
        String flowId = SectionEnvironmentFlow.flowId(sectionId);

        if(flowEngine.getFlows().containsKey(flowId)){
            log.info("flowId={}는 이미 생성된 플로우입니다.", flowId);
            return;
        }

        Flow sectionEnvironmentFlow = sectionEnvironmentFlowFactory.create(sectionId);
        flowEngine.registerAndStart(sectionEnvironmentFlow);
        log.info("플로우를 생성하고 시작했습니다. flowId={}", flowId);
    }

    public void stop(Long sectionId){
        String flowId = SectionEnvironmentFlow.flowId(sectionId);

        if(!flowEngine.getFlows().containsKey(flowId)){
            log.info("중지할 플로우가 존재하지 않습니다. flowId={}", flowId);
            return;
        }

        flowEngine.stopFlow(flowId);
        log.info("플로우를 중지했습니다. flowId={}", flowId);
    }
}
