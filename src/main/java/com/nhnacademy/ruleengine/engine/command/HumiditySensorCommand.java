package com.nhnacademy.ruleengine.engine.command;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorContext;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import org.springframework.stereotype.Component;

@Component
// 습도 측정값을 백분율 센서 payload로 변환한다.
public class HumiditySensorCommand implements SensorCommand {

    @Override
    public String getMeasurementKey() {
        return SensorType.HUMIDITY.value();
    }

    @Override
    public SensorType getSensorType() {
        return SensorType.HUMIDITY;
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
                toDouble(value),
                getSensorType().unit(),
                sensorContext.time()
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
