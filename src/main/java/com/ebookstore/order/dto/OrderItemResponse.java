package com.ebookstore.order.dto;

import java.math.BigDecimal;

/**
 * Response DTO for a single order item.
 *
 * <p>Matches the OpenAPI {@code OrderItemResponse} schema.
 *
 * <p>{@code productTitle} and {@code unitPrice} are the purchase-time snapshots
 * stored in {@code order_items} — NOT the current product values.
 */
public class OrderItemResponse {

    private final Long id;
    private final Long productId;
    private final String productTitle;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public OrderItemResponse(Long id,
                             Long productId,
                             String productTitle,
                             Integer quantity,
                             BigDecimal unitPrice,
                             BigDecimal subtotal) {
        this.id = id;
        this.productId = productId;
        this.productTitle = productTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public Long getId()              { return id; }
    public Long getProductId()       { return productId; }
    public String getProductTitle()  { return productTitle; }
    public Integer getQuantity()     { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal()  { return subtotal; }
}
