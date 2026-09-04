package com.nhnacademy.ruleengine.engine.dto.notification;


import com.nhnacademy.ruleengine.engine.domain.EnvironmentType;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorType;
import com.nhnacademy.ruleengine.engine.dto.sensor.ViolationType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

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

    private static final Set<ViolationType> SAVED_AS_EVENT =
            EnumSet.of(ViolationType.ABOVE_MAX, ViolationType.BELOW_MIN);

    public static Optional<EnvironmentEventCreateRequest> from(NotificationRequest request) {
        // 인벤토리의 BreachType에 없는 위반유형은 환경 이벤트로 저장하지 않는다.
        // 정상 복귀(NORMAL)와 문 상태(OPEN/CLOSED)는 텔레그램으로만 알린다.
        if (!SAVED_AS_EVENT.contains(request.violationType())) {
            return Optional.empty();
        }

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

    private static BigDecimal thresholdValueOf(NotificationRequest request) {
        return switch (request.violationType()) {
            case ABOVE_MAX -> toBigDecimal(request.max());
            case BELOW_MIN -> toBigDecimal(request.min());
            case NORMAL, OPEN, CLOSED -> null;
        };
    }

    private static BigDecimal toBigDecimal(Double value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
