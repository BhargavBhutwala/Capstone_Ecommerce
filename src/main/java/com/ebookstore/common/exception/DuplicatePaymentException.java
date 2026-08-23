package com.ebookstore.common.exception;

/**
 * Thrown when a payment attempt is made for an order that already has a
 * successful or in-progress payment.
 * Maps to HTTP 409 Conflict.
 */
public class DuplicatePaymentException extends RuntimeException {

    public DuplicatePaymentException(String message) {
        super(message);
    }
}
