package com.nhnacademy.ruleengine.engine.dto.environment;

// 문 개폐의 하루 통계.
// door는 열림(1)/닫힘(0) 이진값이라 평균·최소·최대가 의미를 갖지 못하므로
// 다른 센서와 같은 통계를 쓰지 않고 횟수와 누적 시간으로 따로 집계한다.
public record DoorDailyStat(
        long openCount,
        long openMinutes
) {
}
