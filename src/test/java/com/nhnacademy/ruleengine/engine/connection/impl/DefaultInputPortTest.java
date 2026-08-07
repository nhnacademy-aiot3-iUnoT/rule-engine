package com.nhnacademy.ruleengine.engine.connection.impl;

import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.node.Node;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DefaultInputPortTest {

    @Mock
    private Node owner;

    @Test
    @DisplayName("receive()는 fromPort 추가하여 owner.process() 호출한다")
    void receiveMessage() {
        Message message = new Message(new HashMap<>());

        DefaultInputPort port = new DefaultInputPort("in", owner);
        port.receive(message);
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);

        verify(owner).process(captor.capture());
        assertEquals("in", captor.getValue().get("fromPort"));

    }

}