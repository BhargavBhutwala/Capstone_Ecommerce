package com.ebookstore.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /cart/items}.
 *
 * <p>Matches the OpenAPI {@code AddCartItemRequest} schema.
 * {@code userId} is never accepted here — it is extracted from the JWT principal only.
 */
public class AddCartItemRequest {

    @NotNull(message = "productId is required")
    @Min(value = 1, message = "productId must be at least 1")
    private Long productId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be at least 1")
    @Max(value = 999, message = "quantity must not exceed 999")
    private Integer quantity;

    public AddCartItemRequest() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
