package com.team4.core.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(1001, HttpStatus.BAD_REQUEST, "Dữ liệu yêu cầu không hợp lệ"),
    USERNAME_EXISTED(1002, HttpStatus.CONFLICT, "Tên đăng nhập đã tồn tại"),
    EMAIL_EXISTED(1003, HttpStatus.CONFLICT, "Email đã tồn tại"),
    INVALID_CREDENTIALS(1004, HttpStatus.UNAUTHORIZED, "Tên đăng nhập hoặc mật khẩu không đúng"),
    USER_NOT_FOUND(1005, HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"),
    INVALID_ROLE(1006, HttpStatus.BAD_REQUEST, "Chỉ có thể đăng ký vai trò TENANT hoặc LANDLORD"),
    UNAUTHENTICATED(1007, HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện yêu cầu này"),
    ACCESS_DENIED(1008, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện yêu cầu này"),
    UNCATEGORIZED_EXCEPTION(9999, HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
