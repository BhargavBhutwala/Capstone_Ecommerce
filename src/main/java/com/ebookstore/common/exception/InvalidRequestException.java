package com.ebookstore.common.exception;

/**
 * Thrown when a request is semantically invalid in a way not captured by
 * Bean Validation (e.g., an empty cart checkout attempt).
 * Maps to HTTP 400 Bad Request.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
