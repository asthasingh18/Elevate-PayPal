package com.paybridge.exception;

/**
 * Thrown when a payment order request fails validation (e.g. invalid amount, unsupported currency, malformed order ID).
 */
public class InvalidPaymentRequestException extends RuntimeException {
    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}
