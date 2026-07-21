package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.SensorContextDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import org.springframework.stereotype.Component;

@Component
// 도어의 open/close 상태를 1과 0으로 변환한다.
public class DoorSensorCommand implements SensorCommand {

    private static final String MEASUREMENT_KEY = "magnet_status";
    private static final String SENSOR_TYPE = "door";
    private static final String UNIT = "bool";

    @Override
    public String getSensorType() {
        return MEASUREMENT_KEY;
    }

    @Override
    public SensorPayloadDto execute(
            Object value,
            SensorContextDto sensorContextDto
    ) {
        return SensorPayloadDto.fromSensor(
                sensorContextDto.organizationId(),
                sensorContextDto.deviceEui(),
                sensorContextDto.locationId(),
                sensorContextDto.positionId(),
                SENSOR_TYPE,
                toDoorState(value),
                UNIT,
                sensorContextDto.time()
        );
    }

    private double toDoorState(Object value) {
        // open은 1, close는 0으로 저장한다.
        String text = String.valueOf(value).trim();

        if ("open".equalsIgnoreCase(text)) {
            return 1.0;
        }

        if ("close".equalsIgnoreCase(text)) {
            return 0.0;
        }

        throw new IllegalArgumentException(
                "지원하지 않는 도어 상태입니다: " + value
        );
    }
}
