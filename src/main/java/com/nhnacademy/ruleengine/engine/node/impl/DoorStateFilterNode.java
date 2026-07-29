package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.RuleResultCreateRequest;
import com.nhnacademy.ruleengine.engine.dto.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import static com.nhnacademy.ruleengine.engine.constants.MessageFields.RULE_RESULT;
import static com.nhnacademy.ruleengine.engine.constants.MessageFields.SENSOR_PAYLOAD;

//문센서 검사 노드
@Slf4j
public class DoorStateFilterNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final String DOOR = "door";

    public DoorStateFilterNode(String id) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayload sensorPayload = message.get(SENSOR_PAYLOAD);

        if (sensorPayload == null) {
            log.info("[{}] sensorPayload가 없어 문 검사를 건너뜁니다.", getId());
            return;
        }

        String sensorType = sensorPayload.sensorType();
        Long organizationId = sensorPayload.organizationId();
        Long storageId = sensorPayload.storageId();
        Long sectionId = sensorPayload.sectionId();
        double value = sensorPayload.value();

        if(!DOOR.equals(sensorType)){
            return;
        }

        //열림
        if(value==1.0){
            sendRuleResult(message, RuleResultCreateRequest.ofDoorState(sensorPayload, ViolationType.OPEN, true, sensorType + ": 열림"));
            return;
        }

        //닫힘
        if(value==0.0) {
            sendRuleResult(message, RuleResultCreateRequest.ofDoorState(sensorPayload, ViolationType.CLOSED, false, sensorType + ": 닫힘"));
            return;
        }

        log.info("[{}] 지원하지 않는 문 상태 값입니다. organizationId={}, storageId={}, sectionId={}, deviceEui={}, value={}",
                getId(),
                organizationId,
                storageId,
                sectionId,
                sensorPayload.deviceEui(),
                value);
    }

    private void sendRuleResult(
            Message sourceMessage,
            RuleResultCreateRequest request
    ){
        RuleResultDto ruleResult = RuleResultDto.fromDoorState(request);

        send(OUTPUT_PORT, sourceMessage.withPayload(Map.of(
                RULE_RESULT,
                ruleResult
        )));

        log.info("[{}] Door 센서 검사 결과. organizationId={}, storageId={}, sectionId={}, sensorType={}, violate={}, value={}, reason={}",
                getId(),
                request.sensorPayload().organizationId(),
                request.sensorPayload().storageId(),
                request.sensorPayload().sectionId(),
                request.sensorPayload().sensorType(),
                request.violated(),
                request.sensorPayload().value(),
                request.message()
        );
    }
}
