package com.nhnacademy.ruleengine.engine.node.impl.rule;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.dto.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

//문센서 검사 노드
@Slf4j
public class DoorStateFilterNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private static final String SENSOR_PAYLOAD = "sensorPayload";
    private static final String RULE_RESULT = "ruleResult";

    private static final String DOOR = "door";

    public DoorStateFilterNode(String id) {
        super(id);
        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    protected void onProcess(Message message) {
        SensorPayloadDto sensorPayload = message.get(SENSOR_PAYLOAD);

        if (sensorPayload == null) {
            log.info("[{}] sensorPayload가 없어 문 검사를 건너뜁니다.", getId());
            return;
        }

        String sensorType = sensorPayload.sensorType();

        if(!DOOR.equals(sensorType)){
            return;
        }

        Long storageId = sensorPayload.storageId();

        if (storageId == null || storageId <= 0) {
            log.info("[{}] storageId가 없어 문 검사를 건너뜁니다. sensorType={}",
                    getId(),
                    sensorType
            );
            return;
        }

        if (sensorPayload.deviceEui() == null || sensorPayload.deviceEui().isBlank()) {
            log.info("[{}] deviceEui가 없어 문 상태 검사를 건너뜁니다. sensorType={}",
                    getId(), sensorType);
            return;
        }

        if (sensorPayload.value() == null) {
            log.info("[{}] 센서값이 없어 문 검사 검사를 건너뜁니다. locationId={}, sensorType={}",
                    getId(),
                    sensorType
            );
            return;
        }

        double value = sensorPayload.value();

        //열림
        if(value==1.0){
            sendRuleResult(sensorPayload, true, sensorType + ": 열림");
            return;
        }

        //닫힘
        if(value==0.0) {
            sendRuleResult(sensorPayload, false, sensorType + ": 닫힘");
            return;
        }

        log.info("[{}] 지원하지 않는 문 상태 값입니다. locationId={}, deviceEui={}, value={}",
                getId(), sensorPayload.deviceEui(), value);
    }

    private void sendRuleResult(
            SensorPayloadDto sensorPayload,
            boolean violated,
            String message

    ){
        RuleResultDto ruleResult = new RuleResultDto(
                sensorPayload.organizationId(),
                sensorPayload.deviceEui(),
                sensorPayload.storageId(),
                sensorPayload.sectionId(),
                sensorPayload.sensorType(),
                null,
                violated,
                sensorPayload.value(),
                null,
                null,
                sensorPayload.unit(),
                sensorPayload.time(),
                null,
                message
        );

        send(OUTPUT_PORT, new Message(Map.of(
                RULE_RESULT,
                ruleResult
        )));

        log.info("[{}] Door 센서 검사 결과. sensorType={}, violate={}, value={}, reason={}",
                getId(),
                sensorPayload.sensorType(),
                violated,
                sensorPayload.value(),
                message
        );
    }
}
