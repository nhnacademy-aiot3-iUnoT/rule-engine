package com.nhnacademy.ruleengine.engine.connection.impl;

import com.nhnacademy.ruleengine.engine.core.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class LocalConnectionTest {


    @Test
    @DisplayName("null 메시지는 전달할 수 없다")
    void deliverNull() {
        LocalConnection connection = new LocalConnection("test");

        assertThrows(
                NullPointerException.class,
                () -> connection.deliver(null)
        );
    }


    @Test
    @DisplayName("deliver한 메시지는 poll로 조회할 수 있다")
    void deliverAndPoll() throws Exception {
        LocalConnection connection = new LocalConnection("test");

        Message message = new Message(new HashMap<>());

        connection.deliver(message);

        Message result = connection.poll();

        assertSame(message, result);
        assertEquals(0, connection.getBufferSize());
    }

    @Test
    @DisplayName("LocalConnection의 버퍼 크기를 반환한다")
    void returnBufferSize() {
        LocalConnection connection = new LocalConnection("test");

        connection.deliver(new Message(new HashMap<>()));
        connection.deliver(new Message(new HashMap<>()));
        connection.deliver(new Message(new HashMap<>()));


        assertEquals(3, connection.getBufferSize());
    }

}