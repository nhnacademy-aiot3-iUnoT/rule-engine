package com.nhnacademy.ruleengine.engine.command.rule;

import com.nhnacademy.ruleengine.engine.dto.rule.RuleResultDto;
import com.nhnacademy.ruleengine.engine.dto.sensor.SensorPayload;

import java.util.Optional;

// 센서타입별 환경 룰 판단 전략의 공통 규약이다.
// 새 센서타입이 추가될 때 이 인터페이스를 구현한 Bean만 추가하면 되고,
// 디스패치하는 노드나 다른 센서타입의 코드는 건드릴 필요가 없다.
public interface EnvironmentRuleCommand {

    boolean supports(String sensorType);

    // 판단할 룰이 없는 경우(예: 임계값 정책 미설정) Optional.empty()를 반환한다.
    Optional<RuleResultDto> evaluate(SensorPayload sensorPayload);
}
