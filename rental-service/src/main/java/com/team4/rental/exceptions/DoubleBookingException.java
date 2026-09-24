package com.team4.rental.exceptions;

/**
 * Phòng đã có lượt thuê đang hoạt động.
 */
public class DoubleBookingException extends RuntimeException {

    public DoubleBookingException(String message) {
        super(message);
    }
}
