package com.gymproject.payment.payment.infrastructure.persistence;

import com.gymproject.payment.payment.domain.entity.Payment;
import com.gymproject.payment.payment.domain.type.PaymentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long>,
        JpaSpecificationExecutor<Payment> {

    @Query(
     "SELECT p FROM Payment p WHERE p.userId = :userId AND p.snapshotPlanName = :planName AND p.status = :status ORDER BY p.createdAt DESC"
    )
    List<Payment> findRecentPendingPayment(
            @Param("userId") Long userId,
            @Param("planName") String planName,
            @Param("status") PaymentStatus status,
            Pageable pageable
    );

    Optional<Payment> findByPaymentId(Long paymentId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
}
