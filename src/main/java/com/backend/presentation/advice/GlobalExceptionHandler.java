package com.backend.presentation.advice;

import com.backend.domain.dto.response.ApiResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        BusinessError error = ex.getError();
        log.error("Business exception: {} - {}", error.getCode(), error.getMessage());

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(error.getCode())
                .message(error.getMessage())
                .build();

        return ResponseEntity.status(error.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        log.warn("Validation error: {}", message);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(4000)
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(5000)
                .message("Hệ thống đang gặp sự cố, vui lòng thử lại sau")
                .build();

        return ResponseEntity.internalServerError().body(response);
    }
}
