package com.clip.server.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "잘못된 입력값입니다."),
    INSUFFICIENT_WORDS(HttpStatus.BAD_REQUEST, "INSUFFICIENT_WORDS", "퀴즈를 시작하려면 최소 5개의 단어가 필요합니다."),
    INVALID_QUIZ_TYPE(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_TYPE", "올바르지 않은 퀴즈 타입입니다."),
    SESSION_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "SESSION_ALREADY_COMPLETED", "이미 완료된 세션입니다."),
    INVALID_TIMESTAMP(HttpStatus.BAD_REQUEST, "INVALID_TIMESTAMP", "올바른 타임스탬프 형식이 아닙니다."),

    // 401 Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "다시 로그인해주세요."),

    // 403 Forbidden
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
    WORD_NOT_FOUND(HttpStatus.NOT_FOUND, "WORD_NOT_FOUND", "단어를 찾을 수 없습니다."),
    VIDEO_NOT_FOUND(HttpStatus.NOT_FOUND, "VIDEO_NOT_FOUND", "영상을 찾을 수 없습니다."),
    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "퀴즈 세션을 찾을 수 없습니다."),
    PREFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "PREFERENCE_NOT_FOUND", "학습 설정을 찾을 수 없습니다."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", "퀴즈를 찾을 수 없습니다."),

    // 409 Conflict
    WORD_ALREADY_COLLECTED(HttpStatus.CONFLICT, "WORD_ALREADY_COLLECTED", "이미 수집한 단어입니다."),
    PREFERENCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "PREFERENCE_ALREADY_EXISTS", "이미 온보딩을 완료했습니다."),
    VIDEO_ALREADY_EXISTS(HttpStatus.CONFLICT, "VIDEO_ALREADY_EXISTS", "이미 등록된 영상입니다."),

    // 500 Internal Server Error
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
    AI_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AI_SERVICE_ERROR", "AI 서비스 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
