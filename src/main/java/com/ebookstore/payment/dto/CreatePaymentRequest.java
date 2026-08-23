package com.ebookstore.payment.dto;

import com.ebookstore.common.domain.PaymentMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /payments}.
 *
 * <p>The client supplies only the order id and payment method.
 * The payment amount is always taken from {@code order.totalAmount} server-side;
 * the client cannot influence the amount.
 */
public class CreatePaymentRequest {

    @NotNull(message = "orderId is required")
    @Min(value = 1, message = "orderId must be at least 1")
    private Long orderId;

    @NotNull(message = "paymentMethod is required")
    private PaymentMethod paymentMethod;

    public CreatePaymentRequest() {}

    public CreatePaymentRequest(Long orderId, PaymentMethod paymentMethod) {
        this.orderId = orderId;
        this.paymentMethod = paymentMethod;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }
}
