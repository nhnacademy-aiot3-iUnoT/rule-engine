package com.nhnacademy.ruleengine.engine.core;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Getter
// 노드 사이에서 전달되는 불변 메시지 객체다.
public class Message {
    private final UUID uuid;
    private final Map<String, Object> payload;
    private final LocalDateTime timestamp;
    private final FlowProcessingCompletion processingCompletion;

    public Message(Map<String, Object> payload) {
        this(payload, null);
    }

    public Message(
            Map<String, Object> payload,
            FlowProcessingCompletion processingCompletion
    ) {
        this(
                UUID.randomUUID(),
                LocalDateTime.now(),
                payload,
                processingCompletion
        );
    }

    private Message(
            UUID uuid,
            LocalDateTime timestamp,
            Map<String, Object> payload,
            FlowProcessingCompletion processingCompletion
    ) {
        this.uuid = uuid;
        this.timestamp = timestamp;
        this.payload = immutablePayload(payload);
        this.processingCompletion = processingCompletion;
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        return (T) payload.get(key);
    }


    public Message withEntry(String key, Object value) {
        Map<String, Object> newPayload = new LinkedHashMap<>(payload);
        newPayload.put(key, value);

        return derive(newPayload);
    }

    public boolean hasEntry(String key) {
        return payload.get(key) != null;
    }

    public void completeProcessing() {
        if (processingCompletion != null) {
            processingCompletion.complete();
        }
    }

    public void completeProcessingExceptionally(Throwable throwable) {
        if (processingCompletion != null) {
            processingCompletion.completeExceptionally(throwable);
        }
    }

    public Message withPayload(Map<String, Object> newPayload) {
        return derive(newPayload);
    }

    public Message withoutEntry(String key) {
        Map<String, Object> newPayload = new LinkedHashMap<>(payload);
        newPayload.remove(key);

        return derive(newPayload);
    }

    private Message derive(Map<String, Object> newPayload) {
        return new Message(
                uuid,
                timestamp,
                newPayload,
                processingCompletion
        );
    }

    private static Map<String, Object> immutablePayload(
            Map<String, Object> payload
    ) {
        Map<String, Object> source = payload == null ? Map.of() : payload;
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }

    @Override
    public String toString() {
        return "Message{" +
                "uuid=" + uuid +
                ", payload=" + payload +
                ", timestamp=" + timestamp +
                ", processingCompletion=" + processingCompletion +
                '}';
    }
}
