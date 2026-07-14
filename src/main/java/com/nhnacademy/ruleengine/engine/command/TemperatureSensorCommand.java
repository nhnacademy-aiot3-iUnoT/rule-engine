package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.SensorContextDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import org.springframework.stereotype.Component;

@Component
// 온도 측정값을 섭씨 센서 payload로 변환한다.
public class TemperatureSensorCommand implements SensorCommand {

    private static final String SENSOR_TYPE = "temperature";
    private static final String UNIT = "C";

    @Override
    public String getSensorType() {
        return SENSOR_TYPE;
    }

    @Override
    public SensorPayloadDto execute(
            Object value,
            SensorContextDto sensorContextDto
    ) {
        return SensorPayloadDto.fromSensor(
                sensorContextDto.applicationName(),
                sensorContextDto.deviceName(),
                sensorContextDto.deviceEui(),
                sensorContextDto.location(),
                getSensorType(),
                toDouble(value),
                UNIT,
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
