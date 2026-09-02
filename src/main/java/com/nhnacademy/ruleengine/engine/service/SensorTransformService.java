package com.nhnacademy.ruleengine.engine.service;

import com.nhnacademy.ruleengine.engine.command.sensor.SensorCommand;
import com.nhnacademy.ruleengine.engine.dto.ResolvedZoneResponse;
import com.nhnacademy.ruleengine.engine.dto.sensor.ExternalSensorMessage;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SensorTransformService {
    private final Map<String, SensorCommand> commands;
    private final ZoneResolver zoneResolver;

    public SensorTransformService(
            List<SensorCommand> sensorCommands,
            ZoneResolver zoneResolver
    ) {

        this.commands = sensorCommands.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SensorCommand::getMeasurementKey,
                        Function.identity()
                ));
        this.zoneResolver = zoneResolver;
    }

    public List<SensorPayload> transform(
            ExternalSensorMessage externalMessage
    ) {
        if (externalMessage == null
                || externalMessage.measurements() == null
                || externalMessage.measurements().isEmpty()) {
            return List.of();
        }

        Optional<ResolvedZoneResponse> resolvedZone =
                zoneResolver.resolveActive(externalMessage.devEui());

        if (resolvedZone.isEmpty()) {
            log.warn(
                    "구역 정보가 없거나 비활성 구역이라 메시지를 버립니다. devEui={}",
                    externalMessage.devEui()
            );

            return List.of();
        }

        SensorContext context =
                SensorContext.from(externalMessage, resolvedZone.get());

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