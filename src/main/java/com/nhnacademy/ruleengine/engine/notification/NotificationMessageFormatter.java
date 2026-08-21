package com.nhnacademy.ruleengine.engine.notification;

import com.nhnacademy.ruleengine.engine.dto.environment.EnvStatus;
import com.nhnacademy.ruleengine.engine.dto.environment.EnvironmentStatusEventDto;

import java.util.HashMap;
import java.util.Map;

public class NotificationMessageFormatter {

    private NotificationMessageFormatter() {
        /*
            유틸 클래스
         */
    }

    private static final String DOOR_SENSOR_TYPE = "door";

    public static String titleOf(EnvironmentStatusEventDto event){
        return switch (event.currentStatus()){
            case NORMAL -> "환경 상태 정상 복귀";
            case WARNING -> "환경 상태 경고";
            case CRITICAL -> "환경 상태 위험";
        };
    }

    public static String contentOf(EnvironmentStatusEventDto event){
        if(DOOR_SENSOR_TYPE.equals(event.sensorType())){
            return doorContent(event);
        }
        return sensorContent(event);
    }

    private static String doorContent(EnvironmentStatusEventDto event) {
        return String.format(
                "저장소: %s%n구역: %s%n장치: %s%n%s",
                event.storageId(),
                event.zoneId(),
                event.deviceEui(),
                doorStatusMessage(event)
        );
    }

    private static String sensorContent(EnvironmentStatusEventDto event) {
        return String.format(
                "저장소: %s%n구역: %s%n장치: %s%n %s 상태가 %s %n현재값: %.2f%s, %n사유: %s",
                event.storageId(),
                event.zoneId(),
                event.deviceEui(),
                event.sensorType(),
                statusText(event.currentStatus()),
                event.value(),
                nullSafe(event.unit()),
                nullSafe(event.message())
        );
    }

    public static Map<String, Object> metadataOf(EnvironmentStatusEventDto event){
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("value", event.value());
        metadata.put("unit", event.unit());
        metadata.put("message", event.message());
        return metadata;
    }

    private static String doorStatusMessage(EnvironmentStatusEventDto event) {
        if (event.currentStatus() == EnvStatus.NORMAL) {
            return "문이 닫혀 정상으로 복귀했습니다.";
        }

        if (event.currentStatus() == EnvStatus.CRITICAL) {
            return "문이 열렸습니다.";
        }

        return "문 상태 확인이 필요합니다.";
    }

    private static String statusText(EnvStatus status) {
        return switch (status) {
            case NORMAL -> "정상으로 복귀했습니다.";
            case WARNING -> "주의가 필요합니다.";
            case CRITICAL -> "위험합니다.";
        };
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
