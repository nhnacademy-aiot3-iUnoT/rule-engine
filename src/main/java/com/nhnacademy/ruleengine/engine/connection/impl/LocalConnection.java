package com.nhnacademy.ruleengine.engine.connection.impl;


import com.nhnacademy.ruleengine.engine.connection.Connection;
import com.nhnacademy.ruleengine.engine.connection.InputPort;
import com.nhnacademy.ruleengine.engine.core.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
// 메모리 BlockingQueue를 사용하는 로컬 Connection 구현체다.
public class LocalConnection implements Connection {
    String id;
    InputPort target;
    BlockingQueue<Message> buffer;

    public LocalConnection(String id, int capacity) {
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>(capacity);
    }

    public LocalConnection(String id) {
        this.id = id;
        this.buffer = new LinkedBlockingQueue<>();
    }

    @Override
    public void deliver(Message message) {
        // 버퍼가 가득 찬 경우 공간이 생길 때까지 대기한다.
        try {
            buffer.put(message);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public Message poll() {
        // 메시지가 들어올 때까지 대기한 후 하나를 반환한다.
        try {
            return buffer.take();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    @Override
    public void stop() {
        log.debug("Local connection stopping");
    }

    @Override
    public int getBufferSize() {
        return buffer.size();
    }

    @Override
    public void setTarget(InputPort inputPort) {
        this.target = inputPort;
    }

    @Override
    public InputPort getTarget() {
        return this.target;
    }

    @Override
    public String getId() {
        return this.id;
    }


}
