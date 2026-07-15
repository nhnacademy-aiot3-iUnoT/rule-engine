package com.nhnacademy.ruleengine.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "G001", "잘못된 입력값입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "G002", "권한이 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "G003", "서버 오류가 발생했습니다."),

    INVALID_LOCATION_ID(HttpStatus.BAD_REQUEST, "R001", "locationId는 양수여야 합니다."),
    LOCATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "R002", "존재하지 않는 위치 ID입니다."),
    INVALID_SENSOR_TYPE(HttpStatus.BAD_REQUEST, "R003", "sensorType은 필수입니다."),
    UNSUPPORTED_SENSOR_TYPE(HttpStatus.BAD_REQUEST, "R004", "지원하지 않는 센서 종류입니다."),
    SENSOR_DATA_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "센서 데이터 조회에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
