package com.nhnacademy.ruleengine.sensor.exception;

import com.nhnacademy.ruleengine.global.dto.ErrorCode;
import com.nhnacademy.ruleengine.global.exception.BaseException;

public class SensorDataQueryException extends BaseException {

    public SensorDataQueryException(Throwable cause) {
        super(ErrorCode.SENSOR_DATA_QUERY_FAILED, cause);
    }
}
