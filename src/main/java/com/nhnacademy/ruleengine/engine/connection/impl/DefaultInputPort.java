package com.nhnacademy.ruleengine.engine.connection.impl;


import com.nhnacademy.ruleengine.engine.connection.InputPort;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.node.Node;
import lombok.RequiredArgsConstructor;


/**
 * 연결된 이전 노드로부터 메시지를 전달받아
 * 현재 포트의 소유자인 Node에게 메시지를 전달합니다.
 */

@RequiredArgsConstructor
public class DefaultInputPort implements InputPort {

    private final String name; // 입력 포트명
    private final Node owner; // 포트를 소유한 노드

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void receive(Message message) {

        owner.process(message.withEntry("fromPort",this.name));
    }
}
