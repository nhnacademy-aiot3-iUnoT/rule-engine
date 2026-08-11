package com.nhnacademy.ruleengine.engine.connection.impl;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.core.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultOutputPortTest {

    @Mock
    private Connection connection1;

    @Mock
    private Connection connection2;

    @Test
    @DisplayName("연결된 모든 Connection에 메시지 전송")
    void send() {
        DefaultOutputPort outputPort = new DefaultOutputPort("out");

        Message message = new Message(new HashMap<>());
        outputPort.connect(connection1);
        outputPort.connect(connection2);

        outputPort.send(message);

        verify(connection1).deliver(message);
        verify(connection2).deliver(message);
    }

    @Test
    @DisplayName("disconnect()된 Connection에는 메시지를 전달하지 않는다")
    void disconnect() {
        DefaultOutputPort port = new DefaultOutputPort("out");
        Message message = new Message(new HashMap<>());

        port.connect(connection1);
        port.connect(connection2);

        port.disconnect(connection1);

        port.send(message);

        verify(connection2).deliver(message);
        verify(connection1, never()).deliver(any());
    }
}