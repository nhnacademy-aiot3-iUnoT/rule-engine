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

    INVALID_STORAGE_ID(HttpStatus.BAD_REQUEST, "R001", "storageId는 양수여야 합니다."),
    INVALID_SECTION_ID(HttpStatus.BAD_REQUEST, "R002", "sectionId는 양수여야 합니다."),
    SECTION_NOT_FOUND(HttpStatus.BAD_REQUEST, "R003", "존재하지 않는 섹션 ID입니다."),
    STORAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "R004", "존재하지 않는 스토리지 ID 입니다"),
    INVALID_SENSOR_TYPE(HttpStatus.BAD_REQUEST, "R005", "sensorType은 필수입니다."),
    UNSUPPORTED_SENSOR_TYPE(HttpStatus.BAD_REQUEST, "R006", "지원하지 않는 센서 종류입니다."),
    VIRTUAL_SENSOR_FLOW_ALREADY_EXISTS(  HttpStatus.CONFLICT, "R006", "해당 Section의 가상 센서 Flow가 이미 존재합니다."),

    SENSOR_DATA_QUERY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "S001", "센서 데이터 조회에 실패했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
