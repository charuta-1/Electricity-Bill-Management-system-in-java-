package com.msedcl.billing.admin.tariff.repository;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.LateFeePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LateFeePolicyRepository extends JpaRepository<LateFeePolicy, Long> {

    @Query("SELECT l FROM LateFeePolicy l WHERE l.connectionType = :connectionType " +
           "AND l.isActive = true " +
           "AND l.effectiveFrom <= :date " +
           "AND (l.effectiveTo IS NULL OR l.effectiveTo >= :date) " +
           "ORDER BY l.effectiveFrom DESC")
    List<LateFeePolicy> findActivePolicies(Account.ConnectionType connectionType, LocalDate date);
}
