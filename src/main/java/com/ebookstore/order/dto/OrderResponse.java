package com.ebookstore.order.dto;

import com.ebookstore.common.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for an order.
 *
 * <p>Matches the OpenAPI {@code OrderResponse} schema — MVP fields only.
 * Does NOT include {@code giftPointsUsed} or any Phase-2 field.
 *
 * <p>{@code shippingAddress} contains the seven-field snapshot stored on the
 * {@code orders} row at checkout time.
 */
public class OrderResponse {

    private final Long id;
    private final String orderNumber;
    private final OrderStatus status;
    private final List<OrderItemResponse> items;
    private final ShippingAddressSnapshot shippingAddress;
    private final BigDecimal subtotal;
    private final BigDecimal shippingAmount;
    private final BigDecimal discountAmount;
    private final BigDecimal totalAmount;
    private final OffsetDateTime placedAt;
    private final OffsetDateTime cancellationDeadline;

    public OrderResponse(Long id,
                         String orderNumber,
                         OrderStatus status,
                         List<OrderItemResponse> items,
                         ShippingAddressSnapshot shippingAddress,
                         BigDecimal subtotal,
                         BigDecimal shippingAmount,
                         BigDecimal discountAmount,
                         BigDecimal totalAmount,
                         OffsetDateTime placedAt,
                         OffsetDateTime cancellationDeadline) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.status = status;
        this.items = items;
        this.shippingAddress = shippingAddress;
        this.subtotal = subtotal;
        this.shippingAmount = shippingAmount;
        this.discountAmount = discountAmount;
        this.totalAmount = totalAmount;
        this.placedAt = placedAt;
        this.cancellationDeadline = cancellationDeadline;
    }

    public Long getId()                               { return id; }
    public String getOrderNumber()                    { return orderNumber; }
    public OrderStatus getStatus()                    { return status; }
    public List<OrderItemResponse> getItems()         { return items; }
    public ShippingAddressSnapshot getShippingAddress() { return shippingAddress; }
    public BigDecimal getSubtotal()                   { return subtotal; }
    public BigDecimal getShippingAmount()             { return shippingAmount; }
    public BigDecimal getDiscountAmount()             { return discountAmount; }
    public BigDecimal getTotalAmount()                { return totalAmount; }
    public OffsetDateTime getPlacedAt()               { return placedAt; }
    public OffsetDateTime getCancellationDeadline()   { return cancellationDeadline; }
}
