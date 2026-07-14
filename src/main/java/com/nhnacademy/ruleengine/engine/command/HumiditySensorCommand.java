package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.SensorContextDto;
import com.nhnacademy.ruleengine.engine.dto.SensorPayloadDto;
import org.springframework.stereotype.Component;

@Component
// 습도 측정값을 백분율 센서 payload로 변환한다.
public class HumiditySensorCommand implements SensorCommand {

    private static final String SENSOR_TYPE = "humidity";
    private static final String UNIT = "%";

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
                SENSOR_TYPE,
                toDouble(value),
                UNIT,
                sensorContextDto.time()
        );
    }

    private double toDouble(Object value) {
        // 숫자와 숫자 형태의 문자열을 동일하게 변환한다.
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.parseDouble(
                String.valueOf(value).trim()
        );
    }
}
