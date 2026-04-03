package com.clip.server.common.exception;

import com.clip.server.common.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice // 모든 Controller의 예외를 여기서 처리
public class GlobalExceptionHandler {

    // BusinessException 처리(404, 409 등)
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException e
    ) {
        log.error("BusinessException:{}", e.getMessage());

        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status((errorCode.getStatus()))
                .body(ApiResponse.error(errorCode));
    }

    // 모든 예외처리(500)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);

        return ResponseEntity
                .status(500)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
