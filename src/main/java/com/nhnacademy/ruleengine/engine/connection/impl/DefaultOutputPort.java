package com.nhnacademy.ruleengine.engine.connection.impl;


import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.connection.OutputPort;
import com.nhnacademy.ruleengine.engine.core.Message;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 기본 출력 포트 구현체
 *
 * 노드에서 전달받은 메시지를 현재 출력 포트에 연결된
 * 모든 Connection으로 전달합니다.
 *
 * 하나의 출력 포트에 여러 Connection을 연결할 수 있으므로,
 * 하나의 메시지를 여러 노드로 분기하여 전달할 수 있습니다.
 */
@RequiredArgsConstructor
public class DefaultOutputPort implements OutputPort {

    private final List<Connection> connections = new ArrayList<>(); // 출력포트에 연결된 Connections 목록
    private final String name; // 출력포트 이름


    @Override
    public String getName() {
        return name;
    }

    @Override
    public void send(Message message) {
        for (Connection connection : connections) {
            connection.deliver(message);
        }
    }

    @Override
    public void connect(Connection connection) {
        connections.add(connection);

    }

    @Override
    public void disconnect(Connection connection) {
        connections.remove(connection);
    }
}
