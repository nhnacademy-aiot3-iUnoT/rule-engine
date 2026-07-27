package com.nhnacademy.ruleengine.engine.connection;


import com.nhnacademy.ruleengine.engine.core.Message;

// 출력 포트와 입력 포트 사이의 메시지 전달 통로다.
public interface Connection {

    void deliver(Message message);

    Message poll() throws InterruptedException;

    void stop();

    int getBufferSize();

    void setTarget(InputPort inputPort);

    InputPort getTarget();

    String getId();
}
