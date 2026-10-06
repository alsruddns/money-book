package com.moneybook.backend.holiday.client;

public class HolidayApiException extends RuntimeException {
    public HolidayApiException(String message) { super(message); }
    public HolidayApiException(String message, Throwable cause) { super(message, cause); }
}
