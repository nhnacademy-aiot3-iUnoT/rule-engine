package com.nhnacademy.ruleengine.engine.dto.virtual;

import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;

import java.util.Objects;
import java.util.Set;

// 가상 센서 Flow 실행에 필요한 설정을 명확한 타입으로 전달한다.
public record VirtualSensorConfig(
        Long organizationId,
        Long storageId,
        Long zoneId,
        String deviceEui,
        Set<SensorType> enabledSensorTypes,
        Double temperatureMin,
        Double temperatureMax,
        Double humidityMin,
        Double humidityMax,
        Double illuminationMin,
        Double illuminationMax,
        Double doorOpenProbability,
        Long measurementIntervalSeconds
) {

    public static VirtualSensorConfig from(
            Long organizationId,
            Long storageId,
            Long zoneId,
            VirtualSensorCreateRequest request
    ) {
        Objects.requireNonNull(request, "가상 센서 설정은 필수입니다.");

        SensorValueRange temperature = null;
        SensorValueRange humidity = null;
        SensorValueRange illumination = null;

        if (request.enabledSensorTypes().contains(SensorType.TEMPERATURE)) {
            temperature = Objects.requireNonNull(
                    request.temperature(),
                    "온도 범위는 필수입니다."
            );
        }

        if (request.enabledSensorTypes().contains(SensorType.HUMIDITY)) {
            humidity = Objects.requireNonNull(
                    request.humidity(),
                    "습도 범위는 필수입니다."
            );
        }
        if (request.enabledSensorTypes().contains(SensorType.ILLUMINATION)) {
            illumination = Objects.requireNonNull(
                    request.illumination(),
                    "밝기 범위는 필수입니다."
            );
        }
        return new VirtualSensorConfig(
                organizationId,
                storageId,
                zoneId,
                request.deviceEui(),
                request.enabledSensorTypes(),
                temperature != null ? temperature.min() : null,
                temperature != null ? temperature.max() : null,
                humidity !=  null ? humidity.min() : null,
                humidity != null ? humidity.max() : null,
                illumination != null ? illumination.min() : null,
                illumination != null ? illumination.max() : null,
                request.doorOpenProbability(),
                request.measurementIntervalSeconds()
        );
    }

    public static VirtualSensorConfig merge(VirtualSensorConfig existing, VirtualSensorUpdateRequest request) {
        Objects.requireNonNull(existing, "기존 가상 센서 설정은 필수입니다.");
        Objects.requireNonNull(request, "변경할 가상 센서 설정은 필수입니다.");

        SensorValueRange temperature = request.temperature();
        SensorValueRange humidity = request.humidity();
        SensorValueRange illumination = request.illumination();

        return new VirtualSensorConfig(
                existing.organizationId(),
                existing.storageId(),
                existing.zoneId(),
                existing.deviceEui(),
                request.enabledSensorTypes() != null ? request.enabledSensorTypes() : existing.enabledSensorTypes(),
                temperature != null ? temperature.min() : existing.temperatureMin(),
                temperature != null ? temperature.max() : existing.temperatureMax(),
                humidity != null ? humidity.min() : existing.humidityMin(),
                humidity != null ? humidity.max() : existing.humidityMax(),
                illumination != null ? illumination.min() : existing.illuminationMin(),
                illumination != null ? illumination.max() : existing.illuminationMax(),
                request.doorOpenProbability() != null ? request.doorOpenProbability() : existing.doorOpenProbability(),
                request.measurementIntervalSeconds() != null ? request.measurementIntervalSeconds() : existing.measurementIntervalSeconds()
        );
    }
}
