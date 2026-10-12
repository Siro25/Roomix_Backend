package com.team4.rental.exceptions;

public class InvalidMeterReadingException extends AppException {
    public InvalidMeterReadingException() {
        super(ErrorCode.INVALID_METER_READING);
    }
}
