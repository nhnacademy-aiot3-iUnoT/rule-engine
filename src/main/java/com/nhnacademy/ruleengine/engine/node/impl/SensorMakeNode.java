package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.MessageFields;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.dto.SensorType;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
// 설정된 범위와 주기에 따라 가상 센서 데이터를 생성한다.
public class SensorMakeNode extends AbstractNode {

    private static final String OUTPUT_PORT = "out";

    private final double tempMin;
    private final double tempMax;
    private final double humidityMin;
    private final double humidityMax;
    private final double doorOpenProbability;
    private final long measurementInterval;
    private final Long organizationId;
    private final Long storageId;
    private final Long sectionId;
    private final String deviceEui;

    private ScheduledExecutorService scheduler;

    public SensorMakeNode(String nodeId, Map<String, Object> sensorConfig) {
        super(nodeId);

        Map<String, Object> config = Objects.requireNonNull(
                sensorConfig,
                "sensorConfig는 null일 수 없습니다."
        );

        tempMin = requiredDouble(config, "tempMin");
        tempMax = requiredDouble(config, "tempMax");
        humidityMin = requiredDouble(config, "humidityMin");
        humidityMax = requiredDouble(config, "humidityMax");
        doorOpenProbability = requiredDouble(config, "doorOpenProbability");
        measurementInterval = requiredLong(config, "measurementInterval");
        organizationId = requiredLong(config, "organizationId");
        storageId = requiredLong(config, "storageId");
        sectionId = requiredLong(config, "sectionId");
        deviceEui = requiredText(config,"deviceEui");

        validateConfig();
        addOutputPort(OUTPUT_PORT);
    }

    @Override
    public synchronized void initialize() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, getId() + "-sensor-generator");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleAtFixedRate(
                this::generateSafely,
                0,
                measurementInterval,
                TimeUnit.SECONDS
        );
    }

    @Override
    protected void onProcess(Message message) {
        String measuredAt = Instant.now().toString();

        publish(SensorType.TEMPERATURE, randomBetween(tempMin, tempMax), measuredAt);
        publish(SensorType.HUMIDITY, randomBetween(humidityMin, humidityMax), measuredAt);
        publish(
                SensorType.DOOR,
                ThreadLocalRandom.current().nextDouble() < doorOpenProbability ? 1.0 : 0.0,
                measuredAt
        );
        log.info("[{}] 가상 센서 데이터 생성", getId());
    }

    @Override
    public synchronized void shutdown() {
        if (scheduler == null) {
            return;
        }

        scheduler.shutdownNow();
        scheduler = null;
    }

    private void generateSafely() {
        try {
            process(new Message(Map.of()));
        } catch (RuntimeException e) {
            // 한 번의 생성 실패로 스케줄러의 다음 실행까지 중단되지 않게 한다.
            log.error("[{}] 가상 센서 데이터 생성 실패", getId(), e);
        }
    }

    private void publish(SensorType sensorType, double value, String measuredAt) {
        SensorPayloadDto sensorPayload = new SensorPayloadDto(
                organizationId,
                deviceEui,
                storageId,
                sectionId,
                sensorType.value(),
                value,
                sensorType.unit(),
                measuredAt
        );

        String topic = String.format(
                "%d/%d/%d/%s/%s",
                organizationId,
                storageId,
                sectionId,
                sanitize(deviceEui),
                sanitize(sensorType.value())
        );

        send(
                OUTPUT_PORT,
                new Message(Map.of(
                        MessageFields.TOPIC, topic,
                        MessageFields.SENSOR_PAYLOAD, sensorPayload
                ))
        );
    }

    private double randomBetween(double min, double max) {
        if (Double.compare(min, max) == 0) {
            return min;
        }
        return ThreadLocalRandom.current().nextDouble(min, max);
    }

    private void validateConfig() {
        if (!Double.isFinite(tempMin) || !Double.isFinite(tempMax)) {
            throw new IllegalArgumentException("온도 범위는 유한한 숫자여야 합니다.");
        }
        if (!Double.isFinite(humidityMin) || !Double.isFinite(humidityMax)) {
            throw new IllegalArgumentException("습도 범위는 유한한 숫자여야 합니다.");
        }
        if (!Double.isFinite(doorOpenProbability)) {
            throw new IllegalArgumentException("doorOpenProbability는 유한한 숫자여야 합니다.");
        }
        if (tempMin > tempMax) {
            throw new IllegalArgumentException("tempMin은 tempMax보다 클 수 없습니다.");
        }
        if (humidityMin > humidityMax) {
            throw new IllegalArgumentException("humidityMin은 humidityMax보다 클 수 없습니다.");
        }
        if (doorOpenProbability < 0 || doorOpenProbability > 1) {
            throw new IllegalArgumentException("doorOpenProbability는 0 이상 1 이하여야 합니다.");
        }
        if (measurementInterval <= 0) {
            throw new IllegalArgumentException("measurementInterval은 1초 이상이어야 합니다.");
        }
        requirePositive(organizationId, "organizationId");
        requirePositive(storageId, "storageId");
        requirePositive(sectionId, "sectionId");
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }

    private static double requiredDouble(Map<String, Object> config, String key) {
        Object value = requiredValue(config, key);
        try {
            return value instanceof Number number
                    ? number.doubleValue()
                    : Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + "는 숫자여야 합니다.", e);
        }
    }

    private static long requiredLong(Map<String, Object> config, String key) {
        Object value = requiredValue(config, key);
        try {
            return value instanceof Number number
                    ? number.longValue()
                    : Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + "는 정수여야 합니다.", e);
        }
    }

    private static String requiredText(Map<String, Object> config, String key) {
        String value = requiredValue(config, key).toString().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("deviceEui" + "는 비어 있을 수 없습니다.");
        }
        return value;
    }

    private static Object requiredValue(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value == null) {
            throw new IllegalArgumentException("필수 센서 설정이 없습니다: " + key);
        }
        return value;
    }

    private String sanitize(String value) {
        return value.trim().replaceAll("[\\s/]+", "_");
    }
}
