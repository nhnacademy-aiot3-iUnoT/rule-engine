package com.nhnacademy.ruleengine.engine.exception;

// 센서 데이터 저장 실패를 상위 계층에 전달하는 예외다.
public class SensorDataSaveException extends RuntimeException {

    public SensorDataSaveException(String message) {
        super(message);
    }

    public SensorDataSaveException(String message, Throwable cause) {
        super(message, cause);
    }
}
