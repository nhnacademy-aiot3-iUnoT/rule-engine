package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.constants.MessageFields;
import com.nhnacademy.ruleengine.engine.core.Message;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.virtual.VirtualSensorConfig;
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
public class VirtualSensorGeneratorNode extends AbstractNode {

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

    public VirtualSensorGeneratorNode(
            String nodeId,
            VirtualSensorConfig sensorConfig
    ) {
        super(nodeId);

        VirtualSensorConfig config = Objects.requireNonNull(
                sensorConfig,
                "가상 센서 설정은 필수입니다."
        );

        tempMin = config.temperatureMin();
        tempMax = config.temperatureMax();
        humidityMin = config.humidityMin();
        humidityMax = config.humidityMax();
        doorOpenProbability = config.doorOpenProbability();
        measurementInterval = config.measurementIntervalSeconds();
        organizationId = config.organizationId();
        storageId = config.storageId();
        sectionId = config.sectionId();
        deviceEui = config.deviceEui();

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
        SensorPayload sensorPayload = new SensorPayload(
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
        if (deviceEui == null || deviceEui.isBlank()) {
            throw new IllegalArgumentException("deviceEui는 비어 있을 수 없습니다.");
        }
    }

    private void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(fieldName + "는 양수여야 합니다.");
        }
    }

    private String sanitize(String value) {
        return value.trim().replaceAll("[\\s/]+", "_");
    }
}
