package com.ebookstore.common.exception;

/**
 * Thrown when an order cancellation is attempted outside the allowed 48-hour
 * window or when the order status does not permit cancellation.
 * Maps to HTTP 409 Conflict.
 */
public class OrderCancellationNotAllowedException extends RuntimeException {

    public OrderCancellationNotAllowedException(String message) {
        super(message);
    }
}
