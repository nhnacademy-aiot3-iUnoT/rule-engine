package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.dto.RuleResultCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.dto.ThresholdPolicyDto;
import com.nhnacademy.ruleengine.engine.service.ThresholdPolicyService;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

// 임계값 검사 노드
@Slf4j
public class ThresholdFilterNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUT_PORT = "out";

    private static final String SENSOR_PAYLOAD = "sensorPayload";
    private static final String RULE_RESULT = "ruleResult";

    private static final String TEMPERATURE = "temperature";
    private static final String HUMIDITY = "humidity";

    private final ThresholdPolicyService thresholdPolicyService;

    public ThresholdFilterNode(String id,
                               ThresholdPolicyService thresholdPolicyService) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUT_PORT);
        this.thresholdPolicyService = thresholdPolicyService;
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayloadDto sensorPayload = message.get(SENSOR_PAYLOAD);

        if (sensorPayload == null) {
            log.info("[{}] sensorPayload가 없어 임계값 조회를 건너뜁니다.", getId());
            return;
        }

        String sensorType = sensorPayload.sensorType();
        Long organizationId = sensorPayload.organizationId();
        Long storageId = sensorPayload.storageId();
        Long sectionId = sensorPayload.sectionId();


        if(!TEMPERATURE.equals(sensorType) && !HUMIDITY.equals(sensorType)){
            return;
        }

        ThresholdPolicyDto thresholdPolicy = thresholdPolicyService.getThresholdPolicy(organizationId, storageId, sectionId);

        if(thresholdPolicy==null){
            log.info("[{}] 임계값 설정이 없어 검사를 건너뜁니다. organizationId={}, storageId={}, sectionId={}, sensorType={}, deviceEui={}",
                    getId(),
                    organizationId,
                    storageId,
                    sectionId,
                    sensorType,
                    sensorPayload.deviceEui()
            );
            return;
        }

        checkValue(sensorPayload, thresholdPolicy);
    }

    private void checkValue(
            SensorPayloadDto sensorPayload,
            ThresholdPolicyDto thresholdPolicy
    ){
        Double value = sensorPayload.value();
        String sensorType = sensorPayload.sensorType();
        Double min;
        Double max;
        if(TEMPERATURE.equals(sensorType)){
            min = thresholdPolicy.minTemperature();
            max = thresholdPolicy.maxTemperature();
        } else {
            min = thresholdPolicy.minHumidity();
            max = thresholdPolicy.maxHumidity();
        }


        if(min != null && value < min){
            sendRuleResult(RuleResultCreateRequest.ofThreshold(sensorPayload, true, min, max, thresholdPolicy.thresholdDurationMinutes(), sensorType + ": 최솟값 미달"));
            return;
        }

        if(max != null && value > max){
            sendRuleResult(RuleResultCreateRequest.ofThreshold(sensorPayload, true, min, max, thresholdPolicy.thresholdDurationMinutes(), sensorType + ": 최댓값 초과"));
            return;
        }

        if (min == null || max == null) {
            log.info("[{}] 임계값이 없어 검사를 건너뜁니다. organizationId={}, storageId={}, sectionId={}, sensorType={}",
                    getId(),
                    sensorPayload.organizationId(),
                    sensorPayload.storageId(),
                    sensorPayload.sectionId(),
                    sensorPayload.sensorType()
            );
            return;
        }

        sendRuleResult(RuleResultCreateRequest.ofThreshold(sensorPayload, false, min, max, thresholdPolicy.thresholdDurationMinutes(), sensorType + ": 정상"));
    }

    private void sendRuleResult(
            RuleResultCreateRequest ruleResultCreateRequest
    ){
        RuleResultDto ruleResult = RuleResultDto.fromThreshold(ruleResultCreateRequest);

        send(OUT_PORT, new Message(Map.of(
                RULE_RESULT,
                ruleResult
        )));


        log.info("[{}] 임계값 검사 결과. sensorType={}, violate={}, value={}, min={}, max={}, reason={}",
                getId(),
                ruleResultCreateRequest.sensorPayload().sensorType(),
                ruleResultCreateRequest.violated(),
                ruleResultCreateRequest.sensorPayload().value(),
                ruleResultCreateRequest.min(),
                ruleResultCreateRequest.max(),
                ruleResultCreateRequest.message()
        );
    }
}