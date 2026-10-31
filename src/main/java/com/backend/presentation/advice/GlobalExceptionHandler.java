package com.backend.presentation.advice;

import com.backend.presentation.dto.ApiResponse;
import com.backend.domain.exception.BusinessException;
import com.backend.domain.valueobject.BusinessError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String SUPPORTED_METHODS_ATTR = "supportedMethods";

    // ===== Business exception (custom) =====

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

    // ===== 404 - Endpoint not found =====

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("Endpoint not found: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return buildResponse(BusinessError.ENDPOINT_NOT_FOUND);
    }

    // ===== 405 - HTTP method not allowed =====

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not allowed: {} - supported: {}", ex.getMethod(), ex.getSupportedHttpMethods());
        ResponseEntity<ApiResponse<Void>> base = buildResponse(BusinessError.METHOD_NOT_ALLOWED);
        // Attach Allow header so the client knows which methods are valid
        if (ex.getSupportedHttpMethods() != null) {
            return ResponseEntity.status(BusinessError.METHOD_NOT_ALLOWED.getHttpStatus())
                    .allow(ex.getSupportedHttpMethods().toArray(new org.springframework.http.HttpMethod[0]))
                    .body(base.getBody());
        }
        return base;
    }

    // ===== 415 - Unsupported media type =====

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported media type: {}", ex.getContentType());
        return buildResponse(BusinessError.UNSUPPORTED_MEDIA_TYPE);
    }

    // ===== 400 - Bad request variants =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getDefaultMessage())
                .findFirst()
                .orElse("Dữ liệu không hợp lệ");
        log.warn("Validation error: {}", message);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(BusinessError.VALIDATION_FAILED.getCode())
                .message(message)
                .build();

        return ResponseEntity.status(BusinessError.VALIDATION_FAILED.getHttpStatus()).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON: {}", ex.getMostSpecificCause().getMessage());
        return buildResponse(BusinessError.MALFORMED_JSON);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        log.warn("Missing parameter: {}", ex.getParameterName());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(BusinessError.VALIDATION_FAILED.getCode())
                .message("Thiếu tham số bắt buộc: " + ex.getParameterName())
                .build();
        return ResponseEntity.status(BusinessError.VALIDATION_FAILED.getHttpStatus()).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch: {} = {}", ex.getName(), ex.getValue());
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(BusinessError.VALIDATION_FAILED.getCode())
                .message("Tham số không hợp lệ: " + ex.getName())
                .build();
        return ResponseEntity.status(BusinessError.VALIDATION_FAILED.getHttpStatus()).body(response);
    }

    // ===== 401 / 403 - Security exceptions =====

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildResponse(BusinessError.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildResponse(BusinessError.FORBIDDEN);
    }

    // ===== 500 - Generic fallback =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(BusinessError.ERROR_NOT_DEFINE.getCode())
                .message(BusinessError.ERROR_NOT_DEFINE.getMessage())
                .build();

        return ResponseEntity
                .status(BusinessError.ERROR_NOT_DEFINE.getHttpStatus())
                .body(response);
    }

    // ===== Helpers =====

    private ResponseEntity<ApiResponse<Void>> buildResponse(BusinessError error) {
        ApiResponse<Void> body = ApiResponse.<Void>builder()
                .code(error.getCode())
                .message(error.getMessage())
                .build();
        return ResponseEntity.status(error.getHttpStatus()).body(body);
    }
}
