package com.ebookstore.payment.repository;

import com.ebookstore.common.domain.PaymentStatus;
import com.ebookstore.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    boolean existsByOrderIdAndStatusIn(Long orderId, List<PaymentStatus> statuses);
}
