package com.ebookstore.cart.dto;

import com.ebookstore.catalog.dto.ProductSummary;

import java.math.BigDecimal;

/**
 * Response DTO for a single cart item.
 *
 * <p>Matches the OpenAPI {@code CartItemResponse} schema.
 *
 * <p>{@code subtotal} = {@code unitPrice × quantity} — a display-only value;
 * NOT authoritative for checkout totals.
 */
public class CartItemResponse {

    private final Long id;
    private final ProductSummary product;
    private final Integer quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    public CartItemResponse(Long id,
                            ProductSummary product,
                            Integer quantity,
                            BigDecimal unitPrice,
                            BigDecimal subtotal) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = subtotal;
    }

    public Long getId()              { return id; }
    public ProductSummary getProduct() { return product; }
    public Integer getQuantity()     { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal()  { return subtotal; }
}
