package com.nhnacademy.ruleengine.global.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "G001", "잘못된 입력값입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "G002", "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G003", "서버 오류가 발생했습니다."),
    SENSOR_DATA_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "센서 데이터 조회에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
