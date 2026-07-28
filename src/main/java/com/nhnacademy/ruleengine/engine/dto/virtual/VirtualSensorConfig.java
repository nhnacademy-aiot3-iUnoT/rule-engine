package com.nhnacademy.ruleengine.engine.dto.virtual;

import java.util.Objects;

// 가상 센서 Flow 실행에 필요한 설정을 명확한 타입으로 전달한다.
public record VirtualSensorConfig(
        Long organizationId,
        Long storageId,
        Long sectionId,
        String deviceName,
        String deviceEui,
        double temperatureMin,
        double temperatureMax,
        double humidityMin,
        double humidityMax,
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

        return new VirtualSensorConfig(
                organizationId,
                storageId,
                sectionId,
                request.deviceName(),
                request.deviceEui(),
                Objects.requireNonNull(temperature.min(), "온도 최솟값은 필수입니다."),
                Objects.requireNonNull(temperature.max(), "온도 최댓값은 필수입니다."),
                Objects.requireNonNull(humidity.min(), "습도 최솟값은 필수입니다."),
                Objects.requireNonNull(humidity.max(), "습도 최댓값은 필수입니다."),
                Objects.requireNonNull(request.doorOpenProbability(), "문 열림 확률은 필수입니다."),
                Objects.requireNonNull(request.measurementIntervalSeconds(), "측정 주기는 필수입니다.")
        );
    }
}
