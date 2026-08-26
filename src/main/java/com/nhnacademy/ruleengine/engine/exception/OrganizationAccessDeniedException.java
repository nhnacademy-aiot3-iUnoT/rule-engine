package com.nhnacademy.ruleengine.engine.exception;

import com.nhnacademy.ruleengine.global.exception.BaseException;
import com.nhnacademy.ruleengine.global.exception.ErrorCode;

public class OrganizationAccessDeniedException extends BaseException {

    public OrganizationAccessDeniedException(ErrorCode errorCode) {
        super(errorCode);
    }
}
