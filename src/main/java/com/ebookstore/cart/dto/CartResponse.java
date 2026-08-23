package com.ebookstore.cart.dto;

import com.ebookstore.catalog.dto.ProductSummary;
import com.ebookstore.common.domain.CartStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for the authenticated user's cart.
 *
 * <p>Matches the OpenAPI {@code CartResponse} schema.
 *
 * <p>{@code subtotal} and {@code totalAmount} are display-only values
 * calculated from {@code CartItem.unitPrice × quantity}. They are NOT
 * authoritative for checkout — Task 11 (checkout) must re-fetch product
 * prices from {@code products.price}.
 *
 * <p>{@code recommendedProducts} contains at most 4 active, in-stock products
 * derived from the user's purchase history categories, excluding products
 * already in the cart. Returns an empty list when no qualifying products exist.
 */
public class CartResponse {

    private final Long id;
    private final CartStatus status;
    private final List<CartItemResponse> items;
    private final BigDecimal subtotal;
    private final BigDecimal totalAmount;
    private final List<ProductSummary> recommendedProducts;

    public CartResponse(Long id,
                        CartStatus status,
                        List<CartItemResponse> items,
                        BigDecimal subtotal,
                        BigDecimal totalAmount,
                        List<ProductSummary> recommendedProducts) {
        this.id = id;
        this.status = status;
        this.items = items;
        this.subtotal = subtotal;
        this.totalAmount = totalAmount;
        this.recommendedProducts = recommendedProducts;
    }

    public Long getId()                          { return id; }
    public CartStatus getStatus()                { return status; }
    public List<CartItemResponse> getItems()     { return items; }
    public BigDecimal getSubtotal()              { return subtotal; }
    public BigDecimal getTotalAmount()           { return totalAmount; }
    public List<ProductSummary> getRecommendedProducts() { return recommendedProducts; }
}
