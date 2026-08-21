package com.nhnacademy.ruleengine.engine.command.sensor;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
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
    public SensorPayload execute(
            Object value,
            SensorContext sensorContext
    ) {
        return new SensorPayload(
                sensorContext.organizationId(),
                sensorContext.deviceEui(),
                sensorContext.storageId(),
                sensorContext.zoneId(),
                getSensorType().value(),
                toDouble(value),
                getSensorType().unit(),
                sensorContext.time()
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
