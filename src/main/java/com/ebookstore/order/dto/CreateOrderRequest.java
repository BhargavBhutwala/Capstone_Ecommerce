package com.ebookstore.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /orders}.
 *
 * <p>Matches the OpenAPI {@code CreateOrderRequest} schema.
 * Contains ONLY {@code addressId} — no coupon code, no gift points (Phase 2).
 * The authenticated user's id is obtained from the JWT principal only.
 */
public class CreateOrderRequest {

    @NotNull(message = "addressId is required")
    @Min(value = 1, message = "addressId must be at least 1")
    private Long addressId;

    public CreateOrderRequest() {}

    public Long getAddressId() { return addressId; }
    public void setAddressId(Long addressId) { this.addressId = addressId; }
}
