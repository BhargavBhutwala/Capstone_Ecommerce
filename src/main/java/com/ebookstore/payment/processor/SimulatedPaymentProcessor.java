package com.ebookstore.payment.processor;

import com.ebookstore.common.domain.PaymentStatus;
import com.ebookstore.payment.entity.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Simulated payment processor for MVP.
 *
 * <p>Always returns {@link PaymentStatus#SUCCESS} immediately.
 * No external HTTP calls, SDKs, or async processing.
 *
 * <p>Replace this bean with a real gateway implementation in a later phase
 * without modifying the payment domain logic.
 */
@Component
public class SimulatedPaymentProcessor implements PaymentProcessor {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentProcessor.class);

    @Override
    public PaymentStatus process(Payment payment) {
        log.info("SimulatedPaymentProcessor: processing payment reference={}, amount={}",
                payment.getPaymentReference(), payment.getAmount());
        // MVP: always succeed immediately
        return PaymentStatus.SUCCESS;
    }
}
