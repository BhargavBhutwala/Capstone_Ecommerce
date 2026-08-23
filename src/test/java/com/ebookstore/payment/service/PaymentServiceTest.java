package com.ebookstore.payment.service;

import com.ebookstore.common.domain.OrderStatus;
import com.ebookstore.common.domain.PaymentMethod;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PaymentService}.
 * No Spring context — all dependencies are mocked with Mockito.
 * Clock is fixed so time-dependent assertions are deterministic.
 */
@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private PaymentProcessor paymentProcessor;

    private Clock fixedClock;
    private PaymentService paymentService;

    private static final long USER_ID = 1L;
    private static final long ORDER_ID = 10L;
    private static final long PAYMENT_ID = 100L;
    private static final BigDecimal ORDER_TOTAL = new BigDecimal("49.99");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 6, 15, 12, 0, 0);

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(
                Instant.parse("2024-06-15T12:00:00Z"), ZoneOffset.UTC);
        paymentService = new PaymentService(
                paymentRepository, orderRepository, paymentProcessor, fixedClock);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Order buildOrder(OrderStatus status) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNumber("ORD-TESTORDER001");
        order.setStatus(status);
        order.setTotalAmount(ORDER_TOTAL);
        com.ebookstore.user.entity.User user = new com.ebookstore.user.entity.User();
        user.setId(USER_ID);
        order.setUser(user);
        return order;
    }

    private Payment buildPersistedPayment(Order order) {
        Payment payment = new Payment();
        payment.setId(PAYMENT_ID);
        payment.setOrder(order);
        payment.setPaymentReference("PAY-TESTREFERENCE1");
        payment.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        payment.setAmount(order.getTotalAmount());
        payment.setStatus(PaymentStatus.INITIATED);
        return payment;
    }

    private CreatePaymentRequest buildRequest() {
        return new CreatePaymentRequest(ORDER_ID, PaymentMethod.CREDIT_CARD);
    }

    // =========================================================================
    // initiatePayment — happy path
    // =========================================================================

    @Test
    void initiatePayment_successfulPayment_returnsPaymentResponse() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        Payment saved = buildPersistedPayment(order);
        saved.setStatus(PaymentStatus.SUCCESS);
        saved.setPaidAt(FIXED_NOW);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenReturn(saved);
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        PaymentResponse response = paymentService.initiatePayment(USER_ID, buildRequest());

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void initiatePayment_amountEqualsOrderTotalAmount_neverFromClient() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        // Even if some future caller tried to put a different amount, it must use order.totalAmount
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        paymentService.initiatePayment(USER_ID, buildRequest());

        // First save captures the INITIATED payment
        Payment captured = paymentCaptor.getAllValues().get(0);
        assertThat(captured.getAmount()).isEqualByComparingTo(ORDER_TOTAL);
    }

    @Test
    void initiatePayment_paymentStatusBecomesSuccess() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        paymentService.initiatePayment(USER_ID, buildRequest());

        // Second save (after processor) should have SUCCESS status
        Payment finalPayment = paymentCaptor.getAllValues().get(1);
        assertThat(finalPayment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void initiatePayment_orderStatusBecomesPaid() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(orderCaptor.capture())).thenReturn(order);

        paymentService.initiatePayment(USER_ID, buildRequest());

        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void initiatePayment_paidAtIsPopulated() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        paymentService.initiatePayment(USER_ID, buildRequest());

        Payment finalPayment = paymentCaptor.getAllValues().get(1);
        assertThat(finalPayment.getPaidAt()).isNotNull();
        assertThat(finalPayment.getPaidAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void initiatePayment_uniquePaymentReferenceGenerated() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(false);
        when(paymentRepository.save(paymentCaptor.capture())).thenAnswer(inv -> {
            Payment p = inv.getArgument(0);
            p.setId(PAYMENT_ID);
            return p;
        });
        when(paymentProcessor.process(any(Payment.class))).thenReturn(PaymentStatus.SUCCESS);
        when(orderRepository.save(any(Order.class))).thenReturn(order);

        paymentService.initiatePayment(USER_ID, buildRequest());

        Payment captured = paymentCaptor.getAllValues().get(0);
        assertThat(captured.getPaymentReference()).isNotNull()
                .startsWith("PAY-")
                .hasSizeGreaterThan(4);
    }

    // =========================================================================
    // initiatePayment — ownership
    // =========================================================================

    @Test
    void initiatePayment_orderBelongsToAnotherUser_throws404() {
        // findByIdAndUserId returns empty → order not found or belongs to different user
        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, buildRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(paymentRepository, never()).save(any());
    }

    // =========================================================================
    // initiatePayment — status guard
    // =========================================================================

    @Test
    void initiatePayment_orderNotPendingPayment_throwsBusinessRuleViolation() {
        Order order = buildOrder(OrderStatus.PAID); // already paid

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, buildRequest()))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("PENDING_PAYMENT");

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_orderCancelled_throwsBusinessRuleViolation() {
        Order order = buildOrder(OrderStatus.CANCELLED);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, buildRequest()))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_orderConfirmed_throwsBusinessRuleViolation() {
        Order order = buildOrder(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, buildRequest()))
                .isInstanceOf(BusinessRuleViolationException.class);

        verify(paymentRepository, never()).save(any());
    }

    // =========================================================================
    // initiatePayment — duplicate payment protection
    // =========================================================================

    @Test
    void initiatePayment_duplicateSuccessPayment_throws409() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID),
                eq(List.of(PaymentStatus.SUCCESS, PaymentStatus.PROCESSING)))).thenReturn(true);

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, buildRequest()))
                .isInstanceOf(DuplicatePaymentException.class);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void initiatePayment_duplicateProcessingPayment_throws409() {
        Order order = buildOrder(OrderStatus.PENDING_PAYMENT);

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        // Same check covers PROCESSING as well
        when(paymentRepository.existsByOrderIdAndStatusIn(eq(ORDER_ID), anyList())).thenReturn(true);

        assertThatThrownBy(() -> paymentService.initiatePayment(USER_ID, buildRequest()))
                .isInstanceOf(DuplicatePaymentException.class);
    }

    // =========================================================================
    // getPayment
    // =========================================================================

    @Test
    void getPayment_ownPayment_returnsResponse() {
        Order order = buildOrder(OrderStatus.PAID);
        Payment payment = buildPersistedPayment(order);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaidAt(FIXED_NOW);

        when(paymentRepository.findByIdAndUserId(PAYMENT_ID, USER_ID))
                .thenReturn(Optional.of(payment));

        PaymentResponse response = paymentService.getPayment(USER_ID, PAYMENT_ID);

        assertThat(response.getId()).isEqualTo(PAYMENT_ID);
        assertThat(response.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(response.getAmount()).isEqualByComparingTo(ORDER_TOTAL);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(response.getPaidAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void getPayment_anotherUsersPayment_throws404() {
        when(paymentRepository.findByIdAndUserId(PAYMENT_ID, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(USER_ID, PAYMENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPayment_nonExistentPayment_throws404() {
        when(paymentRepository.findByIdAndUserId(999L, USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getPayment(USER_ID, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
