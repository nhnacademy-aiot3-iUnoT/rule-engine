package com.nhnacademy.ruleengine.sensor.node;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import com.nhnacademy.ruleengine.mqtt.dto.MqttInboundMessageDto;
import com.nhnacademy.ruleengine.sensor.command.SensorCommand;
import com.nhnacademy.ruleengine.sensor.dto.SensorContext;
import com.nhnacademy.ruleengine.sensor.dto.SensorPayloadDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
// MQTT 측정값을 센서 타입별 표준 payload로 변환한다.
public class SensorFilterTransformNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";
    private static final String MQTT_INBOUND_KEY = "mqttInbound";

    private final Map<String, SensorCommand> sensorCommands;

    public SensorFilterTransformNode(
            String id,
            List<SensorCommand> sensorCommands
    ) {
        super(id);

        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        // 센서 타입별 변환 전략을 빠르게 찾을 수 있도록 Map으로 구성한다.
        this.sensorCommands = Objects.requireNonNull(
                        sensorCommands,
                        "sensorCommands는 null일 수 없습니다."
                ).stream()
                .collect(Collectors.toUnmodifiableMap(
                        SensorCommand::getSensorType,
                        Function.identity(),
                        (existing, replacement) -> {
                            throw new IllegalArgumentException(
                                    "중복된 센서 타입입니다: " + existing.getSensorType()
                            );
                        }
                ));
    }

    @Override
    public void onProcess(Message message) {
        MqttInboundMessageDto mqttInbound = message.get(MQTT_INBOUND_KEY);

        if (mqttInbound == null) {
            // 변환할 MQTT DTO가 없으면 메시지를 건너뛴다.
            log.warn(
                    "[{}] mqttInbound DTO가 없어 메시지를 건너뜁니다: {}",
                    getId(),
                    message
            );
            return;
        }

        Map<String, Object> measurements = mqttInbound.measurements();

        if (measurements == null || measurements.isEmpty()) {
            // 측정값이 없는 메시지는 다음 노드로 전달하지 않는다.
            log.debug(
                    "[{}] 측정값이 없어 메시지를 건너뜁니다.",
                    getId()
            );
            return;
        }

        SensorContext sensorContext = SensorContext.from(mqttInbound);

        measurements.forEach(
                (sensorType, value) ->
                        executeSensorCommand(
                                sensorType,
                                value,
                                sensorContext
                        )
        );
    }

    private void executeSensorCommand(
            String sensorType,
            Object value,
            SensorContext sensorContext
    ) {
        SensorCommand command = sensorCommands.get(sensorType);

        if (command == null) {
            // 지원하지 않는 센서 타입은 전체 처리를 중단하지 않는다.
            log.debug(
                    "[{}] 지원하지 않는 센서 타입입니다: {}",
                    getId(),
                    sensorType
            );
            return;
        }

        try {
            // 측정값을 표준 센서 payload로 변환해 출력한다.
            sendSensor(command.execute(value, sensorContext));

        } catch (IllegalArgumentException e) {
            log.warn(
                    "[{}] 센서 데이터 변환 실패. type={}, value={}, reason={}",
                    getId(),
                    sensorType,
                    value,
                    e.getMessage()
            );
        } catch (Exception e) {
            log.error(
                    "[{}] 센서 데이터 처리 중 오류 발생. type={}, value={}",
                    getId(),
                    sensorType,
                    value,
                    e
            );
        }
    }

    private void sendSensor(
            SensorPayloadDto sensorPayload
    ) {
        String topic = String.format(
                "%s/%s/%s",
                sanitize(sensorPayload.location()),
                sanitize(sensorPayload.deviceName()),
                sanitize(sensorPayload.sensorType())
        );

        send(OUTPUT_PORT, new Message(
                Map.of(
                        "topic", topic,
                        "sensorPayload", sensorPayload
                )
        ));
    }

    private String sanitize(String value) {
        // MQTT topic 구분자와 공백을 안전한 문자로 바꾼다.
        if (value == null || value.isBlank()) {
            return "unknown";
        }

        return value.trim().replaceAll("[\\s/]+", "_");
    }
}
