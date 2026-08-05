package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog;
import com.nhnacademy.ruleengine.engine.catalog.SectionCatalog.ResolvedSection;
import com.nhnacademy.ruleengine.engine.command.sensor.SensorCommand;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensorTransformService {

    private final SectionCatalog sectionCatalog;
    private final Map<String, SensorCommand> commands;

    public SensorTransformService(
            SectionCatalog sectionCatalog,
            List<SensorCommand> sensorCommands
    ) {
        this.sectionCatalog = sectionCatalog;

        this.commands = sensorCommands.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SensorCommand::getMeasurementKey,
                        Function.identity()
                ));
    }

    public List<SensorPayload> transform(
            ExternalSensorMessage externalMessage
    ) {
        if (externalMessage == null
                || externalMessage.measurements() == null
                || externalMessage.measurements().isEmpty()) {
            return List.of();
        }

        ResolvedSection section = sectionCatalog.resolveSection(
                        externalMessage.applicationName(),
                        externalMessage.location(),
                        externalMessage.point()
                )
                .orElse(null);

        if (section == null) {
            log.warn(
                    "등록되지 않은 센서 위치입니다. application={}, location={}, point={}",
                    externalMessage.applicationName(),
                    externalMessage.location(),
                    externalMessage.point()
            );

            return List.of();
        }

        SensorContext context =
                SensorContext.from(externalMessage, section);

        List<SensorPayload> results = new ArrayList<>();

        externalMessage.measurements().forEach((key, value) -> {

            if (key.equals("magnet_status"))
                key = "door";

            SensorCommand command = commands.get(key);

            if (command == null) {
                log.debug("지원하지 않는 센서 측정값입니다: {}", key);
                return;
            }

            try {
                results.add(command.execute(value, context));
            } catch (IllegalArgumentException exception) {
                log.warn(
                        "센서 변환 실패. key={}, value={}, reason={}",
                        key,
                        value,
                        exception.getMessage()
                );
            }
        });

        return results;
    }
}