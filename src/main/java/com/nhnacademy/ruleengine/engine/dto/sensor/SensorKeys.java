package com.nhnacademy.ruleengine.engine.dto.sensor;

// 센서를 유일하게 식별하는 키를 생성한다.
// 파티션 라우팅(NormalizedSensorPublisher)과 상태 판단 Lock/저장 키(EnvironmentStatusDecisionNode)가
// 같은 센서에 대해 항상 같은 키를 만들어야 하므로, 구성요소 조합을 이 한 곳으로 모은다.
public final class SensorKeys {

    private SensorKeys() {
    }

    public static String of(Long organizationId, Long storageId, Long zoneId, String deviceEui, String sensorType) {
        return zoneOf(organizationId, storageId, zoneId) + ":" +
                sensorFieldOf(deviceEui, sensorType);
    }

    // 구역 단위로 묶어 저장할 때의 키다. 같은 구역 센서들의 상태를 한 번에 읽기 위해 쓴다.
    public static String zoneOf(Long organizationId, Long storageId, Long zoneId) {
        return organizationId + ":" +
                storageId + ":" +
                zoneId;
    }

    // 구역 키 안에서 센서 하나를 구분하는 값이다.
    public static String sensorFieldOf(String deviceEui, String sensorType) {
        return deviceEui + ":" + sensorType;
    }
}
