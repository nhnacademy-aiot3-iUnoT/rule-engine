package com.nhnacademy.ruleengine.engine.connection.impl;

import com.nhnacademy.ruleengine.engine.core.Message;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

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
    @DisplayName("큐 용량을 설정하여 Connection 생성할수있다")
    void createConnectionWithBufferSize() {
        LocalConnection connection = new LocalConnection("test", 10);

        assertNotNull(connection);
    }

    @Test
    @DisplayName("버퍼가 가득 찬 상태에서 스레드가 중단되면 IllegalStateException이 발생한다")
    void deliverInterrupted() {
        // given
        LocalConnection connection = new LocalConnection("test", 1);

        Message message = mock(Message.class);

        connection.deliver(message);

        Thread.currentThread().interrupt();

        // when
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> connection.deliver(message)
        );

        // then
        assertEquals(
                "Connection 메시지 전달 중 중단되었습니다. connectionId=test",
                exception.getMessage()
        );

        assertTrue(Thread.interrupted());
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