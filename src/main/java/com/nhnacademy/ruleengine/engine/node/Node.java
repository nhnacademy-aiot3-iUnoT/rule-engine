package com.nhnacademy.ruleengine.engine.node;


import com.nhnacademy.ruleengine.engine.Message;

// Flow를 구성하는 모든 노드의 공통 생명주기를 정의한다.
public interface Node {
    String getId();

    void process(Message message);

    void initialize();

    void shutdown();
}
