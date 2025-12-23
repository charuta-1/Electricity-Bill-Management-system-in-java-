package com.msedcl.billing.admin.tariff.repository;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.SubsidyRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubsidyRuleRepository extends JpaRepository<SubsidyRule, Long> {

    @Query("SELECT s FROM SubsidyRule s WHERE s.tariffCode = :tariffCode " +
           "AND s.connectionType = :connectionType " +
           "AND s.isActive = true " +
           "AND s.effectiveFrom <= :date " +
           "AND (s.effectiveTo IS NULL OR s.effectiveTo >= :date) " +
           "ORDER BY s.effectiveFrom DESC")
    List<SubsidyRule> findActiveRules(String tariffCode, Account.ConnectionType connectionType, LocalDate date);
}
