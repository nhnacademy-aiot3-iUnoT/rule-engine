package com.nhnacademy.ruleengine.engine.node.impl;



import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.engine.dto.ExternalSensorMessageDto;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
// 메시지 내용을 기록한 후 변경 없이 다음 노드로 전달한다.
public class LogNode extends AbstractNode {


    public LogNode(String id) {
        super(id);
        addInputPort("in");
        addOutputPort("out");

    }


    @Override
    protected void onProcess(Message message) {
        // MQTT DTO가 있으면 일반 payload 대신 DTO 내용을 기록한다.
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        ExternalSensorMessageDto mqttInbound = message.get("mqttInbound");

        if (mqttInbound != null) {
            log.info("[{}][{}] {}", LocalDateTime.now().format(dtf), getId(), mqttInbound);
            send("out", message);
            return;
        }

        log.info("[{}][{}] {}", LocalDateTime.now().format(dtf),getId(),message);

        send("out",message);
    }
}
