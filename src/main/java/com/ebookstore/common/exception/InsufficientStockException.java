package com.ebookstore.common.exception;

/**
 * Thrown when a product does not have enough stock to fulfil a request.
 * Maps to HTTP 409 Conflict.
 */
public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String message) {
        super(message);
    }
}
