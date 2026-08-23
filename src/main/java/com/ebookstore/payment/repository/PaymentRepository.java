package com.ebookstore.payment.repository;

import com.ebookstore.common.domain.PaymentStatus;
import com.ebookstore.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    boolean existsByOrderIdAndStatusIn(Long orderId, List<PaymentStatus> statuses);

    /**
     * Loads a payment by id and verifies that the associated order belongs to the
     * given user — prevents one customer from reading another's payment.
     */
    @Query("SELECT p FROM Payment p JOIN p.order o WHERE p.id = :paymentId AND o.user.id = :userId")
    Optional<Payment> findByIdAndUserId(@Param("paymentId") Long paymentId, @Param("userId") Long userId);
}
