package com.nhnacademy.ruleengine.engine.core;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;

@Getter
// 노드 사이에서 전달되는 불변 메시지 객체다.
public class Message {
    private final UUID uuid;
    private final Map<String, Object> payload;
    private final LocalDateTime timestamp;

    public Message(Map<String, Object> payload) {
        // 외부 Map 변경이 메시지에 영향을 주지 않도록 복사한다.
        this.uuid = UUID.randomUUID();
        this.payload = Collections.unmodifiableMap(new LinkedHashMap<>(payload == null ? Map.of() : payload));
        this.timestamp = LocalDateTime.now();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) payload.get(key);
    }


    public Message withEntry(String key, Object value) {
        // 기존 메시지는 유지하고 값이 추가된 새 메시지를 반환한다.
        Map<String, Object> newPayload = new HashMap<>(this.payload);
        newPayload.put(key, value);

        return new Message(newPayload);
    }

    public boolean hasEntry(String key) {
        return payload.get(key) != null;
    }

    public Message withoutEntry(String key) {
        // 기존 메시지는 유지하고 값이 제거된 새 메시지를 반환한다.
        Map<String, Object> newPayload = new HashMap<>(this.payload);
        newPayload.remove(key);
        return new Message(newPayload);

    }

    @Override
    public String toString() {
        return payload.toString();
    }
}
