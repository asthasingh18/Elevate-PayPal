package com.paybridge.exception;

/**
 * Thrown when a supplied QR payload fails validation or is malformed.
 */
public class InvalidQrException extends RuntimeException {

    public InvalidQrException(String message) {
        super(message);
    }

    public InvalidQrException(String message, Throwable cause) {
        super(message, cause);
    }
}
