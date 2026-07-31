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
    private final double illuminationMin;
    private final double illuminationMax;
    private final double doorOpenProbability;
    private final long measurementInterval;
    private final Long organizationId;
    private final Long storageId;
    private final Long sectionId;
    private final String deviceEui;

    // 실제 door 센서처럼 "상태가 바뀔 때만" 이벤트를 전송하기 위해 마지막으로 전송한 상태를 기억한다.
    // 초기값은 "닫힘(0.0)"으로 가정한다.
    private volatile double lastDoorValue = 0.0;

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
        illuminationMin = config.illuminationMin();
        illuminationMax = config.illuminationMax();
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

        // 매번 새로 시작할 때는 "닫힘" 상태부터 다시 시작한다.
        lastDoorValue = 0.0;

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

        publish(SensorType.TEMPERATURE,
                randomBetween(tempMin, tempMax),
                measuredAt);

        publish(SensorType.HUMIDITY,
                randomBetween(humidityMin, humidityMax),
                measuredAt);

        publishDoorIfChanged(measuredAt);

        publish(SensorType.ILLUMINATION,
                randomBetween(illuminationMin, illuminationMax),
                measuredAt);

        log.debug("[{}] 가상 센서 데이터 생성", getId());
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

    /**
     * 실제 door 센서처럼 상태가 바뀔 때만 이벤트를 전송한다.
     * <p>
     * 매 tick마다 doorOpenProbability 확률로 "열림" 여부를 다시 뽑아보고,
     * 직전에 전송했던 상태와 같으면 아무것도 보내지 않는다(실제 센서가 상태 변화 없을 때
     * 아무 데이터도 보내지 않는 것과 동일한 동작).
     * 값이 달라진 경우에만 publish하고 마지막 상태를 갱신한다.
     */
    private void publishDoorIfChanged(String measuredAt) {
        double nextDoorValue = ThreadLocalRandom.current().nextDouble() < doorOpenProbability
                ? 1.0
                : 0.0;

        if (Double.compare(nextDoorValue, lastDoorValue) == 0) {
            return;
        }

        lastDoorValue = nextDoorValue;

        publish(SensorType.DOOR, nextDoorValue, measuredAt);
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
        if (tempMin > tempMax) {
            throw new IllegalArgumentException("tempMin은 tempMax보다 클 수 없습니다.");
        }
        if (humidityMin > humidityMax) {
            throw new IllegalArgumentException("humidityMin은 humidityMax보다 클 수 없습니다.");
        }
        if(illuminationMin > illuminationMax) {
            throw new IllegalArgumentException("illuminationMin은 illuminationMax보다 클 수 없습니다.");
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