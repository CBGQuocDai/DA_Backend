package com.backend.domain.valueobject;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessError {
    // System errors
    ERROR_NOT_DEFINE(5000, "Hệ thống đang gặp sự cố, vui lòng thử lại sau",
        HttpStatus.INTERNAL_SERVER_ERROR),

    // Authentication errors
    USER_NOT_FOUND(4001, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(4002, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    LOGIN_FAIL(4003, "Email hoặc mật khẩu không đúng", HttpStatus.BAD_REQUEST),
    USER_EXISTED(4004, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    PASSWORD_WRONG(4005, "Mật khẩu không đúng", HttpStatus.BAD_REQUEST);
    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    BusinessError(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
