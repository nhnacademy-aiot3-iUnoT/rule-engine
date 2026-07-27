package com.nhnacademy.ruleengine.engine.connection;

import com.nhnacademy.ruleengine.engine.core.Message;

// Connection에서 받은 메시지를 소유 노드에 전달하는 입력 지점이다.
public interface InputPort {
    String getName();
    void receive(Message message);

}
