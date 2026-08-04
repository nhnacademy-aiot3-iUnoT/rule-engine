package com.nhnacademy.ruleengine.testsupport;

import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.connection.InputPort;
import com.nhnacademy.ruleengine.engine.core.Message;

// 노드 결과 캡쳐용 테스트헬퍼
public class CapturingConnection implements Connection {
    private Message message;

    @Override
    public void deliver(Message message) {
        this.message = message;
    }

    public Message captured() {
        return message;
    }

    public boolean hasMessage() {
        return message != null;
    }

    public void clear() {
        this.message = null;
    }

    @Override public Message poll() { return message; }
    @Override public void stop() {}
    @Override public int getBufferSize() { return message == null ? 0 : 1; }
    @Override public void setTarget(InputPort inputPort) {}
    @Override public InputPort getTarget() { return null; }
    @Override public String getId() { return "capturing"; }
}
