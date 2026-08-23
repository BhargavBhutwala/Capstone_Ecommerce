package com.ebookstore.common.domain;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    CONFIRMED,
    CANCELLED,
    SHIPPED,
    DELIVERED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED
}
