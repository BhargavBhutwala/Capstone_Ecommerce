package com.ebookstore.payment.controller;

import com.ebookstore.payment.dto.CreatePaymentRequest;
import com.ebookstore.payment.dto.PaymentResponse;
import com.ebookstore.payment.service.PaymentService;
import com.ebookstore.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Payment endpoints.
 *
 * <p>All endpoints require a valid JWT. The authenticated user's database id is
 * obtained exclusively from {@link AuthenticatedUser#getId()} — never from
 * request body, path variables, or query parameters.
 *
 * <p>Payment amount is always derived server-side from {@code order.totalAmount};
 * clients cannot supply or influence the charge amount.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * operationId: initiatePayment
     *
     * <p>Initiates payment for an order. Returns 201 on success, 409 on conflict
     * (duplicate payment or order not in PENDING_PAYMENT state).
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> initiatePayment(
            @Valid @RequestBody CreatePaymentRequest request,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        PaymentResponse response = paymentService.initiatePayment(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * operationId: getPayment
     *
     * <p>Returns payment status for the given id. Returns 404 if not found or
     * if the payment does not belong to the authenticated user.
     */
    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable Long paymentId,
            Authentication authentication) {
        Long userId = ((AuthenticatedUser) authentication.getPrincipal()).getId();
        return ResponseEntity.ok(paymentService.getPayment(userId, paymentId));
    }
}
