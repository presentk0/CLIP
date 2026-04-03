package com.clip.server.common.response;

import com.clip.server.common.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드 제외
public class ApiResponse<T> {
    //성공 응답
    private boolean success;
    private T data;
    private String message;
    private ErrorInfo error;

    // 성공 응답 - data만
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    // 성공 응답 - data/message
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    // 에러 응답
    public static <T> ApiResponse<T> error(ErrorCode errorCode) {
        return new ApiResponse<>(false, null, null,
                new ErrorInfo(errorCode.getCode(), errorCode.getMessage()));
    }

    // 에러 응답 - 커스텀 메시지
    public static <T> ApiResponse<T> error(ErrorCode errorCode, String message) {
        return new ApiResponse<>(false, null, null,
                new ErrorInfo(errorCode.getCode(), message));
    }

    @Getter
    @AllArgsConstructor
    public static class ErrorInfo() {
        private String code;
        private String message;
    }
}
