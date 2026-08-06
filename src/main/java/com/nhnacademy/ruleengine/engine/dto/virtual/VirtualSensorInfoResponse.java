package com.nhnacademy.ruleengine.engine.dto.virtual;

public record VirtualSensorInfoResponse(

        String deviceEui,

        Long measurementIntervalSeconds,

        SensorValueRange temperature,

        SensorValueRange humidity,

        SensorValueRange illumination,

        Double doorOpenProbability
) {
    public static VirtualSensorInfoResponse from(VirtualSensorConfig config) {
        return new VirtualSensorInfoResponse(
                config.deviceEui(),
                config.measurementIntervalSeconds(),
                new SensorValueRange(config.temperatureMin(), config.temperatureMax()),
                new SensorValueRange(config.humidityMin(), config.humidityMax()),
                new SensorValueRange(config.illuminationMin(), config.illuminationMax()),
                config.doorOpenProbability()
        );
    }
}