package com.ebookstore.payment.service;

import com.ebookstore.common.domain.OrderStatus;
import com.ebookstore.common.domain.PaymentStatus;
import com.ebookstore.common.exception.BusinessRuleViolationException;
import com.ebookstore.common.exception.DuplicatePaymentException;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.order.entity.Order;
import com.ebookstore.order.repository.OrderRepository;
import com.ebookstore.payment.dto.CreatePaymentRequest;
import com.ebookstore.payment.dto.PaymentResponse;
import com.ebookstore.payment.entity.Payment;
import com.ebookstore.payment.processor.PaymentProcessor;
import com.ebookstore.payment.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Business logic for payment initiation and retrieval.
 *
 * <p>Transaction boundaries:
 * <ul>
 *   <li>{@link #initiatePayment} — single {@code @Transactional} wrapping the full payment flow</li>
 *   <li>{@link #getPayment} — {@code @Transactional(readOnly = true)}</li>
 * </ul>
 *
 * <p>Payment amount is always taken from {@code order.totalAmount}; the client cannot
 * supply or influence the amount.
 *
 * <p>The MVP transition on success:
 * <pre>
 *   Order: PENDING_PAYMENT → PAID
 *   Payment: INITIATED → SUCCESS
 * </pre>
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    /**
     * Statuses that indicate a payment is already in progress or complete — a
     * second attempt for the same order is a duplicate.
     */
    private static final List<PaymentStatus> BLOCKING_STATUSES =
            List.of(PaymentStatus.SUCCESS, PaymentStatus.PROCESSING);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentProcessor paymentProcessor;
    private final Clock clock;

    public PaymentService(PaymentRepository paymentRepository,
                          OrderRepository orderRepository,
                          PaymentProcessor paymentProcessor,
                          Clock clock) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentProcessor = paymentProcessor;
        this.clock = clock;
    }

    // =========================================================================
    // operationId: initiatePayment
    // =========================================================================

    /**
     * Initiates payment for the specified order — fully atomic via a single transaction.
     *
     * <p>Steps:
     * <ol>
     *   <li>Verify ownership: load order via {@code findByIdAndUserId}; 404 if not found.</li>
     *   <li>Verify order status is {@code PENDING_PAYMENT}; 409 otherwise.</li>
     *   <li>Check for duplicate: 409 if a SUCCESS or PROCESSING payment already exists.</li>
     *   <li>Generate a unique {@code paymentReference}.</li>
     *   <li>Create {@link Payment} with {@code status = INITIATED}, {@code amount = order.totalAmount}.</li>
     *   <li>Persist the payment.</li>
     *   <li>Delegate to {@link PaymentProcessor}.</li>
     *   <li>Set {@code payment.status = SUCCESS}, {@code payment.paidAt = now}.</li>
     *   <li>Set {@code order.status = PAID}.</li>
     *   <li>Persist changes and return {@link PaymentResponse}.</li>
     * </ol>
     */
    @Transactional
    public PaymentResponse initiatePayment(Long userId, CreatePaymentRequest request) {

        // 1. Verify ownership — 404 if order doesn't exist or belongs to another user
        Order order = orderRepository.findByIdAndUserId(request.getOrderId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order not found with id: " + request.getOrderId()));

        // 2. Verify order is in PENDING_PAYMENT status
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessRuleViolationException(
                    "Payment can only be initiated for orders in PENDING_PAYMENT status. "
                            + "Current status: " + order.getStatus());
        }

        // 3. Duplicate payment protection: block if SUCCESS or PROCESSING already exists
        if (paymentRepository.existsByOrderIdAndStatusIn(order.getId(), BLOCKING_STATUSES)) {
            throw new DuplicatePaymentException(
                    "A payment for order " + order.getOrderNumber()
                            + " has already been initiated or completed.");
        }

        // 4. Generate a unique payment reference
        String paymentReference = "PAY-" + UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();

        // 5. Create Payment with INITIATED status and amount = order.totalAmount (server-authoritative)
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setPaymentReference(paymentReference);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setAmount(order.getTotalAmount());   // always from order — never from client
        payment.setStatus(PaymentStatus.INITIATED);

        // 6. Persist
        payment = paymentRepository.save(payment);

        // 7. Delegate to payment processor
        PaymentStatus outcome = paymentProcessor.process(payment);

        // 8 & 9. Update payment and order based on outcome
        LocalDateTime now = LocalDateTime.now(clock);
        payment.setStatus(outcome);

        if (outcome == PaymentStatus.SUCCESS) {
            payment.setPaidAt(now);
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            log.info("Payment successful: paymentId={}, paymentReference={}, orderId={}, userId={}, amount={}",
                    payment.getId(), paymentReference, order.getId(), userId, payment.getAmount());
        } else {
            log.warn("Payment outcome non-SUCCESS: paymentId={}, status={}, orderId={}, userId={}",
                    payment.getId(), outcome, order.getId(), userId);
        }

        // 10. Persist final payment state and return
        payment = paymentRepository.save(payment);
        return toResponse(payment);
    }

    // =========================================================================
    // operationId: getPayment
    // =========================================================================

    /**
     * Returns a payment by id, verifying that the associated order belongs to
     * the authenticated user.
     *
     * @throws ResourceNotFoundException if the payment is not found or belongs to another user
     */
    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndUserId(paymentId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment not found with id: " + paymentId));
        return toResponse(payment);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    static PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getPaymentReference(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
