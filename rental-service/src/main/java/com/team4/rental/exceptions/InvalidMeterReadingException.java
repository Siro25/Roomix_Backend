package com.team4.rental.exceptions;

/**
 * Chỉ số điện hoặc nước mới nhỏ hơn chỉ số cũ.
 */
public class InvalidMeterReadingException extends RuntimeException {

    public InvalidMeterReadingException(String message) {
        super(message);
    }
}
