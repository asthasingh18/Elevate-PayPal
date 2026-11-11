package com.paybridge.exception;

/**
 * Thrown when a compatibility evaluation request contains invalid or missing fields.
 */
public class InvalidEvaluationRequestException extends RuntimeException {

    public InvalidEvaluationRequestException(String message) {
        super(message);
    }
}
