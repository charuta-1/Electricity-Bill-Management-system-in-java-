package com.msedcl.billing.admin.account.repository;

import com.msedcl.billing.shared.entity.Account;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);
    Optional<Account> findByMeterNumber(String meterNumber);
    @EntityGraph(attributePaths = {"customer", "customer.user"})
    List<Account> findAllByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"customer", "customer.user"})
    Optional<Account> findByAccountId(Long accountId);
    @EntityGraph(attributePaths = {"customer", "customer.user"})
    List<Account> findByCustomerCustomerId(Long customerId);
    @EntityGraph(attributePaths = {"customer", "customer.user"})
    List<Account> findByCustomerCustomerIdAndIsActiveTrue(Long customerId);
    Boolean existsByAccountNumber(String accountNumber);
    Boolean existsByMeterNumber(String meterNumber);
    long countByIsActiveTrue();
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    Optional<Account> findTopByOrderByMeterNumberDesc();
}
