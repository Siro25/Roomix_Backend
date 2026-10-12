package com.team4.rental.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    INVALID_REQUEST(2001, HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),
    UNAUTHENTICATED(2002, HttpStatus.UNAUTHORIZED, "Token không hợp lệ hoặc tài khoản không hoạt động"),
    ACCESS_DENIED(2003, HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác"),
    NOT_FOUND(2004, HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu thuộc tài khoản"),
    ROOM_UNAVAILABLE(2005, HttpStatus.CONFLICT, "Phòng chưa được duyệt hoặc không còn khả dụng"),
    DOUBLE_BOOKING(2006, HttpStatus.CONFLICT, "Phòng đã có hợp đồng PENDING hoặc ACTIVE"),
    DUPLICATE_REQUEST(2007, HttpStatus.CONFLICT, "Bạn đã có yêu cầu đang chờ xử lý cho phòng này"),
    INVALID_STATE(2008, HttpStatus.CONFLICT, "Trạng thái hiện tại không cho phép thao tác"),
    INVALID_METER_READING(2009, HttpStatus.BAD_REQUEST, "Chỉ số mới phải lớn hơn hoặc bằng chỉ số cũ và nối tiếp kỳ trước"),
    INVALID_BILLING_MONTH(2010, HttpStatus.BAD_REQUEST, "Kỳ hóa đơn phải liên tiếp và nằm trong thời gian thuê"),
    DUPLICATE_INVOICE(2011, HttpStatus.CONFLICT, "Hợp đồng đã có hóa đơn cho kỳ này"),
    INVOICE_DISPUTED(2012, HttpStatus.CONFLICT, "Cần giải quyết khiếu nại trước khi xác nhận hoặc thu tiền"),
    CORE_UNAVAILABLE(2013, HttpStatus.SERVICE_UNAVAILABLE, "Không thể xác minh dữ liệu tại Core Service"),
    DATA_CONFLICT(2014, HttpStatus.CONFLICT, "Dữ liệu đã thay đổi hoặc vi phạm ràng buộc duy nhất"),
    INTERNAL_ERROR(2099, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");
    private final int code;
    private final HttpStatus status;
    private final String message;
}
