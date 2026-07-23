package com.nhnacademy.ruleengine.engine.node.impl;

import com.nhnacademy.ruleengine.engine.Message;
import com.nhnacademy.ruleengine.engine.MessageFields;
import com.nhnacademy.ruleengine.engine.command.SensorCommand;
import com.nhnacademy.ruleengine.engine.dto.ExternalSensorMessageDto;
import com.nhnacademy.ruleengine.engine.dto.SensorContextDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import com.nhnacademy.ruleengine.engine.location.SectionCatalog;
import com.nhnacademy.ruleengine.engine.location.SectionCatalog.ResolvedSection;
import com.nhnacademy.ruleengine.engine.node.AbstractNode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
// MQTT 측정값을 센서 타입별 표준 payload로 변환한다.
public class SensorTransformNode extends AbstractNode {

    private static final String INPUT_PORT = "in";
    private static final String OUTPUT_PORT = "out";

    private final Map<String, SensorCommand> sensorCommands;
    private final SectionCatalog sectionCatalog;

    public SensorTransformNode(
            String id,
            List<SensorCommand> sensorCommands,
            SectionCatalog sectionCatalog
    ) {
        super(id);
        this.sectionCatalog = Objects.requireNonNull(
                sectionCatalog,
                "sectionCatalog은 null일 수 없습니다."
        );

        addInputPort(INPUT_PORT);
        addOutputPort(OUTPUT_PORT);

        // 센서 타입별 변환 전략을 빠르게 찾을 수 있도록 Map으로 구성한다.
        this.sensorCommands = Objects.requireNonNull(
                        sensorCommands,
                        "sensorCommands는 null일 수 없습니다."
                ).stream()
                .collect(Collectors.toUnmodifiableMap(
                        SensorCommand::getMeasurementKey,
                        Function.identity(),
                        (existing, replacement) -> {
                            throw new IllegalArgumentException(
                                    "중복된 측정 키입니다: " + existing.getMeasurementKey()
                            );
                        }
                ));
    }

    @Override
    public void onProcess(Message message) {
        ExternalSensorMessageDto externalSensorMessage = message.get(MessageFields.EXTERNAL_SENSOR_MESSAGE);

        if (externalSensorMessage == null) {
            // 변환할 MQTT DTO가 없으면 메시지를 건너뛴다.
            log.warn(
                    "[{}] 외부 센서 메시지가 없어 변환을 건너뜁니다: {}",
                    getId(),
                    message
            );
            return;
        }

        Map<String, Object> measurements = externalSensorMessage.measurements();

        if (measurements == null || measurements.isEmpty()) {
            // 측정값이 없는 메시지는 다음 노드로 전달하지 않는다.
            log.debug(
                    "[{}] 측정값이 없어 메시지를 건너뜁니다.",
                    getId()
            );
            return;
        }

        ResolvedSection resolvedSection = sectionCatalog.resolve(
                        externalSensorMessage.applicationName(),
                        externalSensorMessage.location(),
                        externalSensorMessage.point()
                )
                .orElse(null);

        if (resolvedSection == null) {
            log.warn(
                    "[{}] 등록되지 않은 센서 위치입니다. applicationName={}, location={}, point={}",
                    getId(),
                    externalSensorMessage.applicationName(),
                    externalSensorMessage.location(),
                    externalSensorMessage.point()
            );
            return;
        }

        SensorContextDto sensorContextDto = SensorContextDto.from(
                externalSensorMessage,
                resolvedSection
        );

        measurements.forEach(
                (sensorType, value) ->
                        executeSensorCommand(
                                sensorType,
                                value,
                                sensorContextDto
                        )
        );
    }

    private void executeSensorCommand(
            String sensorType,
            Object value,
            SensorContextDto sensorContextDto
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
            sendSensor(command.execute(value, sensorContextDto));

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
                "%d/%d/%s/%s",
                sensorPayload.organizationId(),
                sensorPayload.storageId(),
                sanitize(sensorPayload.deviceEui()),
                sanitize(sensorPayload.sensorType())
        );

        send(OUTPUT_PORT, new Message(
                Map.of(
                        MessageFields.TOPIC, topic,
                        MessageFields.SENSOR_PAYLOAD, sensorPayload
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
