package com.nhnacademy.ruleengine.engine.exception;

import com.nhnacademy.ruleengine.global.exception.BaseException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;

public class VirtualSensorFlowException extends BaseException {

    public VirtualSensorFlowException(ErrorCode errorCode) {
        super(errorCode);
    }
}