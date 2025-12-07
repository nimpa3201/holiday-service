package com.planitsquare.holidayservice.global.exception;

import com.planitsquare.holidayservice.global.api.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleBusinessException(BusinessException e) {

        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse body = new ErrorResponse(errorCode.name(), e.getMessage());

        log.warn("[BusinessException] code={}, message={}", errorCode.name(), e.getMessage());

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.error(body));
    }

    /**
     * Bean Validation 예외 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleValidationException(MethodArgumentNotValidException e) {

        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .findFirst()
            .map(err -> err.getField() + " " + err.getDefaultMessage())
            .orElse("요청 값이 유효하지 않습니다.");

        ErrorResponse body = new ErrorResponse(ErrorCode.INVALID_REQUEST.name(), message);

        return ResponseEntity
            .status(ErrorCode.INVALID_REQUEST.getStatus())
            .body(ApiResponse.error(body));
    }

    /**
     * 처리되지 않은 예외
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleException(Exception e) {
        log.error("[UnhandledException]", e);

        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;

        ErrorResponse body = new ErrorResponse(errorCode.name(), errorCode.getMessage());

        return ResponseEntity
            .status(errorCode.getStatus())
            .body(ApiResponse.error(body));
    }
}
