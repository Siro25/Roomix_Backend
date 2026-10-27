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
    DEPOSIT_EXPIRED(2015, HttpStatus.CONFLICT, "Đã hết hạn xác nhận tiền cọc"),
    SETTLEMENT_REQUIRED(2016, HttpStatus.CONFLICT, "Cần hoàn tất quyết toán trước khi kết thúc hợp đồng"),
    CORRECTION_NOT_ALLOWED(2017, HttpStatus.CONFLICT, "Chỉ được sửa hóa đơn kỳ mới nhất chưa thu tiền của hợp đồng ACTIVE"),
    PAYMENT_MISMATCH(2018, HttpStatus.CONFLICT, "Số tiền đã thay đổi hoặc chưa thu đủ/hoàn đủ theo quyết toán"),
    NOT_OVERDUE(2019, HttpStatus.CONFLICT, "Hóa đơn không còn quá hạn cần nhắc"),
    VNPAY_NOT_CONFIGURED(2020, HttpStatus.SERVICE_UNAVAILABLE, "Chưa cấu hình tài khoản VNPAY sandbox"),
    VNPAY_PAYMENT_PENDING(2021, HttpStatus.CONFLICT, "Hóa đơn đang có giao dịch VNPAY chờ kết quả"),
    VNPAY_AMOUNT_INVALID(2022, HttpStatus.BAD_REQUEST, "Số tiền hóa đơn không phù hợp để thanh toán VNPAY"),
    LANDLORD_CONTACT_INCOMPLETE(
            2023,
            HttpStatus.CONFLICT,
            "Chủ trọ cần cập nhật số điện thoại và Zalo trước khi chấp nhận yêu cầu"),
    CONTACT_CARD_NOT_FOUND(2024, HttpStatus.NOT_FOUND, "Không tìm thấy danh thiếp cho yêu cầu thuê này"),
    INTERNAL_ERROR(2099, HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");
    private final int code;
    private final HttpStatus status;
    private final String message;
}
