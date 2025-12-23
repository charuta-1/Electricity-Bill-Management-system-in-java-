package com.msedcl.billing.admin.billing.repository;

import com.msedcl.billing.shared.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByInvoiceNumber(String invoiceNumber);
    List<Bill> findByAccountAccountIdOrderByBillDateDesc(Long accountId);
    Optional<Bill> findTopByAccountAccountIdOrderByBillDateDesc(Long accountId);
    Optional<Bill> findByAccountAccountIdAndBillMonth(Long accountId, String billMonth);
    List<Bill> findByBillStatus(Bill.BillStatus status);

    @Query("SELECT b FROM Bill b WHERE b.dueDate < :date AND b.billStatus IN ('UNPAID', 'PARTIALLY_PAID')")
    List<Bill> findOverdueBills(LocalDate date);

    @Query("SELECT COALESCE(SUM(b.balanceAmount), 0) FROM Bill b WHERE b.billStatus IN ('UNPAID', 'PARTIALLY_PAID')")
    BigDecimal sumOutstandingAmount();

    @Query("SELECT COALESCE(SUM(b.balanceAmount), 0) FROM Bill b WHERE b.account.customer.customerId = :customerId " +
        "AND b.billStatus IN ('UNPAID', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal sumOutstandingAmountByCustomer(@Param("customerId") Long customerId);

    @Query("SELECT COALESCE(SUM(b.balanceAmount), 0) FROM Bill b WHERE b.account.accountId = :accountId " +
        "AND b.billStatus IN ('UNPAID', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal sumOutstandingAmountByAccount(@Param("accountId") Long accountId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.billDate BETWEEN :start AND :end")
    BigDecimal sumBilledBetween(LocalDate start, LocalDate end);

    long countByBillDateBetween(LocalDate start, LocalDate end);

    long countByBillStatus(Bill.BillStatus status);

    long countByBillStatusIn(Collection<Bill.BillStatus> statuses);

    List<Bill> findByDueDateBetweenAndBillStatusIn(LocalDate start, LocalDate end, Collection<Bill.BillStatus> statuses);

    List<Bill> findByBillStatusInAndDueDateBefore(Collection<Bill.BillStatus> statuses, LocalDate date);

    List<Bill> findByAccountCustomerCustomerIdOrderByBillDateDesc(Long customerId);

    Optional<Bill> findTopByAccountCustomerCustomerIdOrderByBillDateDesc(Long customerId);

    List<Bill> findTop5ByAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(Long customerId, Collection<Bill.BillStatus> statuses);

    List<Bill> findByAccountCustomerCustomerIdAndBillStatusIn(Long customerId, Collection<Bill.BillStatus> statuses);

    List<Bill> findTop6ByAccountCustomerCustomerIdOrderByBillDateDesc(Long customerId);

    Optional<Bill> findTopByAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(Long customerId, Collection<Bill.BillStatus> statuses);
}
