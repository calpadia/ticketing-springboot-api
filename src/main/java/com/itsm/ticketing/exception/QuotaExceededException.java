package com.itsm.ticketing.exception;

/**
 * Custom exception thrown when a client's maintenance quota has been exhausted.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
