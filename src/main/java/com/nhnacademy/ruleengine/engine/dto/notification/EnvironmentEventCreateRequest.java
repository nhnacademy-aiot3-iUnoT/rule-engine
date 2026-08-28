package com.nhnacademy.ruleengine.engine.dto.notification;


import com.nhnacademy.ruleengine.engine.domain.EnvironmentType;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Optional;

// 인벤토리의 환경 이벤트 생성 요청. 인벤토리의 EnvironmentEventCreateRequest와 같은 모양이어야 한다.
// breachType은 인벤토리의 BreachType과 값이 같아서 ViolationType을 그대로 보낸다.
public record EnvironmentEventCreateRequest(

        @NotNull(message = "구역아이디는 필수입력 사항입니다.")
        Long zoneId,

        @NotNull(message = "감지된 값은 필수입력 사항입니다.")
        BigDecimal detectedValue,

        @NotNull(message = "임계값은 필수입력 사항입니다.")
        BigDecimal thresholdValue,

        @NotNull(message = "환경유형은 필수입력 사항입니다.")
        EnvironmentType environmentType,

        @NotNull(message = "위반유형은 필수입력 사항입니다.")
        ViolationType breachType
) {
    // 문 센서는 임계값 개념이 없어서 닫힘(0)을 기준값으로 보낸다.
    private static final BigDecimal DOOR_THRESHOLD = BigDecimal.ZERO;

    public static Optional<EnvironmentEventCreateRequest> from(NotificationRequest request) {
        EnvironmentType environmentType = SensorType.findByValue(request.sensorType())
                .map(SensorType::environmentType)
                .orElse(null);

        BigDecimal thresholdValue = thresholdValueOf(request);

        if (environmentType == null || thresholdValue == null || request.value() == null) {
            return Optional.empty();
        }

        return Optional.of(new EnvironmentEventCreateRequest(
                request.zoneId(),
                BigDecimal.valueOf(request.value()),
                thresholdValue,
                environmentType,
                request.violationType()
        ));
    }

    // 위반한 쪽의 임계값을 보낸다. 정상 복귀는 어느 쪽을 넘었었는지 알 수 없어 설정된 임계값을 그대로 보낸다.
    private static BigDecimal thresholdValueOf(NotificationRequest request) {
        return switch (request.violationType()) {
            case ABOVE_MAX -> toBigDecimal(request.max());
            case BELOW_MIN -> toBigDecimal(request.min());
            case NORMAL -> toBigDecimal(request.max() == null ? request.min() : request.max());
            case OPEN, CLOSED -> DOOR_THRESHOLD;
        };
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
