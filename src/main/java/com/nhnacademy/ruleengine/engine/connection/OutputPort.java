package com.nhnacademy.ruleengine.engine.connection;


import com.nhnacademy.ruleengine.engine.core.Message;

// 노드의 메시지를 연결된 Connection으로 보내는 출력 지점이다.
public interface OutputPort {

    String getName();

    // 실제로 메시지를 전달한 Connection 수를 반환한다.
    // 0이면 아무도 이어받지 않았다는 뜻이라, 노드가 처리 완료를 스스로 알릴 수 있다.
    int send(Message message);

    void connect(Connection connection);
    void disconnect(Connection connection);

}
