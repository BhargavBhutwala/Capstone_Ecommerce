package com.ebookstore.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code PUT /cart/items/{itemId}}.
 *
 * <p>Matches the OpenAPI {@code UpdateCartItemRequest} schema.
 */
public class UpdateCartItemRequest {

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 999, message = "quantity must not exceed 999")
    private Integer quantity;

    public UpdateCartItemRequest() {}

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
