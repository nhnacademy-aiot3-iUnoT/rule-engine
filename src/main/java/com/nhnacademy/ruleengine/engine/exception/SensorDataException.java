package com.nhnacademy.ruleengine.engine.exception;

import com.nhnacademy.ruleengine.global.exception.BaseException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;

public class SensorDataException extends BaseException {

    public SensorDataException(ErrorCode errorCode) {
        super(errorCode);
    }
}
