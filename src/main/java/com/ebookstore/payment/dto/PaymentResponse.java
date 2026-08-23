package com.ebookstore.payment.dto;

import com.ebookstore.common.domain.PaymentMethod;
import com.ebookstore.common.domain.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for payment endpoints.
 *
 * <p>Exposes exactly the fields defined by the OpenAPI {@code PaymentResponse} schema:
 * {@code id}, {@code orderId}, {@code paymentReference}, {@code paymentMethod},
 * {@code amount}, {@code status}, {@code paidAt}.
 *
 * <p>Internal entity fields are never exposed through this DTO.
 * Amount is {@link BigDecimal} — never {@code double} or {@code float}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private String paymentReference;
    private PaymentMethod paymentMethod;
    private BigDecimal amount;
    private PaymentStatus status;
    private LocalDateTime paidAt;

    public PaymentResponse() {}

    public PaymentResponse(Long id, Long orderId, String paymentReference,
                           PaymentMethod paymentMethod, BigDecimal amount,
                           PaymentStatus status, LocalDateTime paidAt) {
        this.id = id;
        this.orderId = orderId;
        this.paymentReference = paymentReference;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getPaymentReference() { return paymentReference; }
    public void setPaymentReference(String paymentReference) { this.paymentReference = paymentReference; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
}
