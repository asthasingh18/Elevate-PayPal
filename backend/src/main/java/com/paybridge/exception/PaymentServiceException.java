package com.paybridge.exception;

/**
 * Thrown when an interaction with the PayPal payment gateway fails or when payment service is unavailable.
 */
public class PaymentServiceException extends RuntimeException {
    public PaymentServiceException(String message) {
        super(message);
    }

    public PaymentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
