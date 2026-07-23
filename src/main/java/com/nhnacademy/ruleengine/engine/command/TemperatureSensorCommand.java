package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.SensorType;
import com.nhnacademy.ruleengine.engine.dto.SensorContextDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import org.springframework.stereotype.Component;

@Component
// 온도 측정값을 섭씨 센서 payload로 변환한다.
public class TemperatureSensorCommand implements SensorCommand {

    @Override
    public String getMeasurementKey() {
        return SensorType.TEMPERATURE.value();
    }

    @Override
    public SensorType getSensorType() {
        return SensorType.TEMPERATURE;
    }

    @Override
    public SensorPayloadDto execute(
            Object value,
            SensorContextDto sensorContextDto
    ) {
        return SensorPayloadDto.fromSensor(
                sensorContextDto.organizationId(),
                sensorContextDto.deviceEui(),
                sensorContextDto.storageId(),
                sensorContextDto.sectionId(),
                getSensorType().value(),
                toDouble(value),
                getSensorType().unit(),
                sensorContextDto.time()
        );
    }

    private double toDouble(Object value) {
        // 숫자와 숫자 형태의 문자열을 모두 허용한다.
        if (value == null) {
            throw new IllegalArgumentException("온도 값이 null입니다.");
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(
                String.valueOf(value).trim()
        );
    }
}
