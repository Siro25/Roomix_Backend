package com.team4.rental.exceptions;

public class DoubleBookingException extends AppException {
    public DoubleBookingException() {
        super(ErrorCode.DOUBLE_BOOKING);
    }
}
