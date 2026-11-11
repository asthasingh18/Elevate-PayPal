package com.paybridge.exception;

/**
 * Thrown when a requested resource (e.g. evaluationId) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
