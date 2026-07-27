package com.nhnacademy.ruleengine.engine.connection;


import com.nhnacademy.ruleengine.engine.core.Message;

// 노드의 메시지를 연결된 Connection으로 보내는 출력 지점이다.
public interface OutputPort {

    String getName();
    void send(Message message);
    void connect(Connection connection);
    void disconnect(Connection connection);

}
