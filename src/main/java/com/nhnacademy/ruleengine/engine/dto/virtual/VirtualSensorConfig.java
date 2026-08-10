package com.nhnacademy.ruleengine.engine.dto.virtual;

import java.util.Objects;

// 가상 센서 Flow 실행에 필요한 설정을 명확한 타입으로 전달한다.
public record VirtualSensorConfig(
        Long organizationId,
        Long storageId,
        Long sectionId,
        String deviceEui,
        double temperatureMin,
        double temperatureMax,
        double humidityMin,
        double humidityMax,
        double illuminationMin,
        double illuminationMax,
        double doorOpenProbability,
        long measurementIntervalSeconds
) {

    public static VirtualSensorConfig from(
            Long organizationId,
            Long storageId,
            Long sectionId,
            VirtualSensorCreateRequest request
    ) {
        Objects.requireNonNull(request, "가상 센서 설정은 필수입니다.");

        SensorValueRange temperature = Objects.requireNonNull(
                request.temperature(),
                "온도 범위는 필수입니다."
        );
        SensorValueRange humidity = Objects.requireNonNull(
                request.humidity(),
                "습도 범위는 필수입니다."
        );
        SensorValueRange illumination = Objects.requireNonNull(
                request.illumination(),
                "밝기 범위는 필수입니다."
        );

        return new VirtualSensorConfig(
                organizationId,
                storageId,
                sectionId,
                request.deviceEui(),
                Objects.requireNonNull(temperature.min(), "온도 최솟값은 필수입니다."),
                Objects.requireNonNull(temperature.max(), "온도 최댓값은 필수입니다."),
                Objects.requireNonNull(humidity.min(), "습도 최솟값은 필수입니다."),
                Objects.requireNonNull(humidity.max(), "습도 최댓값은 필수입니다."),
                Objects.requireNonNull(illumination.min(), "밝기 최솟값은 필수입니다."),
                Objects.requireNonNull(illumination.max(), "밝기 최댓값은 필수입니다."),
                Objects.requireNonNull(request.doorOpenProbability(), "문 열림 확률은 필수입니다."),
                Objects.requireNonNull(request.measurementIntervalSeconds(), "측정 주기는 필수입니다.")
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
                existing.sectionId(),
                existing.deviceEui(),
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
