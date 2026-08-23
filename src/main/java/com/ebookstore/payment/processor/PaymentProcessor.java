package com.ebookstore.payment.processor;

import com.ebookstore.common.domain.PaymentStatus;
import com.ebookstore.payment.entity.Payment;

/**
 * Abstraction over a payment gateway.
 *
 * <p>The MVP uses {@link SimulatedPaymentProcessor} which always returns
 * {@link PaymentStatus#SUCCESS}. A real gateway implementation can be introduced
 * later by providing an alternative bean without changing the payment domain.
 */
public interface PaymentProcessor {

    /**
     * Process the given payment and return the resulting {@link PaymentStatus}.
     *
     * @param payment the payment to process (must have amount and paymentMethod set)
     * @return the outcome status ({@code SUCCESS}, {@code FAILED}, etc.)
     */
    PaymentStatus process(Payment payment);
}
