package com.msedcl.billing.user.payment.repository;

import com.msedcl.billing.shared.entity.Payment;
import com.msedcl.billing.user.payment.dto.MonthlyCollectionProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    List<Payment> findByBill_BillIdOrderByPaymentDateDesc(Long billId);
    List<Payment> findByAccountAccountIdOrderByPaymentDateDesc(Long accountId);
    List<Payment> findByAccountCustomerCustomerIdOrderByPaymentDateDesc(Long customerId);
    List<Payment> findByPaymentStatus(Payment.PaymentStatus status);

    Optional<Payment> findTopByAccountCustomerCustomerIdOrderByPaymentDateDesc(Long customerId);

    List<Payment> findTop5ByAccountCustomerCustomerIdOrderByPaymentDateDesc(Long customerId);

    @Query("SELECT COALESCE(SUM(p.paymentAmount), 0) FROM Payment p WHERE p.paymentStatus = 'SUCCESS' AND p.paymentDate BETWEEN :start AND :end")
    BigDecimal sumSuccessfulPaymentsBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT YEAR(p.paymentDate) AS year, MONTH(p.paymentDate) AS month, COALESCE(SUM(p.paymentAmount), 0) AS totalAmount " +
           "FROM Payment p WHERE p.paymentStatus = 'SUCCESS' AND p.paymentDate >= :since " +
           "GROUP BY YEAR(p.paymentDate), MONTH(p.paymentDate) ORDER BY year, month")
    List<MonthlyCollectionProjection> findMonthlyCollections(LocalDateTime since);
}
