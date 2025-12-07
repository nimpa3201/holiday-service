package com.planitsquare.holidayservice.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "검색 조건이 유효하지 않습니다."),
    DELETE_CONDITION_REQUIRED(HttpStatus.BAD_REQUEST, "삭제를 위해 year 또는 countryCode 중 하나 이상이 필요합니다."),
    SYNC_TARGET_NOT_FOUND(HttpStatus.BAD_REQUEST, "동기화 대상이 존재하지 않습니다."),

    COUNTRY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 국가 정보를 찾을 수 없습니다."),
    HOLIDAY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 공휴일 정보를 찾을 수 없습니다."),

    NAGER_API_ERROR(HttpStatus.BAD_GATEWAY, "외부 API(Nager.Date) 요청 중 오류가 발생했습니다."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}