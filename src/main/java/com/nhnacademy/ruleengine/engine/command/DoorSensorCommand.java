package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import org.springframework.stereotype.Component;

@Component
// 도어의 open/close 상태를 1과 0으로 변환한다.
public class DoorSensorCommand implements SensorCommand {

    private static final String MEASUREMENT_KEY = "magnet_status";

    @Override
    public String getMeasurementKey() {
        return MEASUREMENT_KEY;
    }

    @Override
    public SensorType getSensorType() {
        return SensorType.DOOR;
    }

    @Override
    public SensorPayload execute(
            Object value,
            SensorContext sensorContext
    ) {
        return new SensorPayload(
                sensorContext.organizationId(),
                sensorContext.deviceEui(),
                sensorContext.storageId(),
                sensorContext.sectionId(),
                getSensorType().value(),
                toDoorState(value),
                getSensorType().unit(),
                sensorContext.time()
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
