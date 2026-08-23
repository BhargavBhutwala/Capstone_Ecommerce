package com.ebookstore.common.exception;

/**
 * Thrown when a general business rule is violated that does not fit a more
 * specific exception class.
 * Maps to HTTP 409 Conflict.
 */
public class BusinessRuleViolationException extends RuntimeException {

    public BusinessRuleViolationException(String message) {
        super(message);
    }
}
