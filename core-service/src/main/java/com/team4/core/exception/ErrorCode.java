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
    INVALID_USER_FILTER(1009, HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ lọc người dùng TENANT hoặc LANDLORD"),
    USER_STATUS_UNCHANGED(1010, HttpStatus.CONFLICT, "Tài khoản đã ở trạng thái yêu cầu"),
    INVALID_USER_STATUS_TRANSITION(1011, HttpStatus.CONFLICT, "Không thể chuyển trạng thái tài khoản theo yêu cầu"),
    LANDLORD_REQUIRED(1012, HttpStatus.BAD_REQUEST, "Tài khoản được chọn không phải chủ trọ"),
    LANDLORD_NOT_PENDING(1013, HttpStatus.CONFLICT, "Hồ sơ chủ trọ không ở trạng thái chờ xác minh"),
    ADMIN_ACCOUNT_MANAGEMENT_NOT_ALLOWED(1014, HttpStatus.BAD_REQUEST, "Không quản lý tài khoản Admin tại API này"),
    POST_NOT_FOUND(1015, HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng"),
    INVALID_POST_FILTER(1016, HttpStatus.BAD_REQUEST, "Chỉ hỗ trợ xem bài đăng đang chờ duyệt"),
    POST_NOT_PENDING(1017, HttpStatus.CONFLICT, "Bài đăng không ở trạng thái chờ duyệt"),
    FLOOR_NOT_FOUND(1018, HttpStatus.NOT_FOUND, "Không tìm thấy tầng thuộc quyền quản lý của bạn"),
    ROOM_NOT_FOUND(1019, HttpStatus.NOT_FOUND, "Không tìm thấy phòng thuộc quyền quản lý của bạn"),
    ROOM_POST_NOT_FOUND(1020, HttpStatus.NOT_FOUND, "Không tìm thấy bài đăng của phòng"),
    ROOM_UPDATE_NOT_ALLOWED(1021, HttpStatus.CONFLICT, "Chỉ được sửa phòng đang lưu nháp hoặc bị từ chối"),
    INVALID_POST_STATUS_TRANSITION(1022, HttpStatus.CONFLICT, "Không thể chuyển trạng thái bài đăng theo yêu cầu"),
    ROOM_SUBMISSION_INCOMPLETE(1023, HttpStatus.BAD_REQUEST, "Phòng cần có giá, diện tích và ít nhất một hình ảnh trước khi gửi duyệt"),
    INVALID_ROOM_IMAGE(1024, HttpStatus.BAD_REQUEST, "Tệp tải lên phải là hình ảnh hợp lệ"),
    IMAGE_UPLOAD_FAILED(1025, HttpStatus.BAD_GATEWAY, "Không thể tải hình ảnh lên Cloudinary"),
    UNCATEGORIZED_EXCEPTION(9999, HttpStatus.INTERNAL_SERVER_ERROR, "Đã xảy ra lỗi hệ thống");

    private final int code;
    private final HttpStatus status;
    private final String message;
}
