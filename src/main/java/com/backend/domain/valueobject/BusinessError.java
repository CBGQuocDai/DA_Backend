package com.backend.domain.valueobject;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BusinessError {
    // System errors
    ERROR_NOT_DEFINE(5000, "Hệ thống đang gặp sự cố, vui lòng thử lại sau",
        HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND(4040, "Không tìm thấy tài nguyên", HttpStatus.NOT_FOUND),

    // HTTP / Routing errors
    ENDPOINT_NOT_FOUND(4041, "Endpoint không tồn tại", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(4050, "Phương thức HTTP không được hỗ trợ",
        HttpStatus.METHOD_NOT_ALLOWED),
    UNSUPPORTED_MEDIA_TYPE(4150, "Định dạng dữ liệu gửi lên không được hỗ trợ",
        HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    MALFORMED_JSON(4009, "Dữ liệu JSON không đúng định dạng", HttpStatus.BAD_REQUEST),
    VALIDATION_FAILED(4099, "Dữ liệu không hợp lệ", HttpStatus.BAD_REQUEST),

    // Authentication / Authorization errors
    UNAUTHORIZED(4010, "Bạn chưa đăng nhập hoặc phiên đã hết hạn",
        HttpStatus.UNAUTHORIZED),
    FORBIDDEN(4030, "Bạn không có quyền truy cập tài nguyên này",
        HttpStatus.FORBIDDEN),

    // Authentication errors
    USER_NOT_FOUND(4001, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(4002, "Email hoặc mật khẩu không đúng", HttpStatus.UNAUTHORIZED),
    LOGIN_FAIL(4003, "Email hoặc mật khẩu không đúng", HttpStatus.BAD_REQUEST),
    USER_EXISTED(4004, "Email đã tồn tại", HttpStatus.BAD_REQUEST),
    PASSWORD_WRONG(4005, "Mật khẩu không đúng", HttpStatus.BAD_REQUEST),
    BOOK_NOT_FOUND(4006, "Không tìm thấy sách", HttpStatus.BAD_REQUEST),
    BOOK_ALREADY_FAVORITED(4007, "Sách đã có trong danh sách yêu thích", HttpStatus.BAD_REQUEST),
    BOOK_FAVORITE_NOT_FOUND(4008, "Sách không có trong danh sách yêu thích", HttpStatus.BAD_REQUEST),

//    File không đúng định dạng
    FILE_FORMAT_INCORRECT(4100, "File tải lên không đúng định dạng", HttpStatus.BAD_REQUEST),
    VOICE_TOO_LARGE(4101, "File giong mẫu phải <= 10MB", HttpStatus.BAD_REQUEST),
    INVALID_AUDIO_EXTENSION(4102, "Hệ thống chỉ hỗ trợ định dạng mp3 và wav", HttpStatus.BAD_REQUEST),

    // Voice validation errors
    VOICE_NAME_REQUIRED(4200, "Tên giọng không được để trống", HttpStatus.BAD_REQUEST),
    VOICE_NAME_TOO_LONG(4201, "Tên giọng không được dài quá 255 kí tự", HttpStatus.BAD_REQUEST),
    VOICE_DESCRIPTION_TOO_LONG(4202, "Mô tả giọng không được dài quá 255 kí tự", HttpStatus.BAD_REQUEST),
    ;
    private final Integer code;
    private final String message;
    private final HttpStatus httpStatus;

    BusinessError(Integer code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}