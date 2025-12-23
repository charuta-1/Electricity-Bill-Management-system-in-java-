package com.msedcl.billing.admin.account.repository;

import com.msedcl.billing.admin.account.dto.MonthlyConsumptionProjection;
import com.msedcl.billing.shared.entity.MeterReading;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, Long> {
    @EntityGraph(attributePaths = {"account", "account.customer", "recordedBy"})
    List<MeterReading> findByAccountAccountIdOrderByReadingDateDesc(Long accountId);
    List<MeterReading> findTop5ByAccountAccountIdOrderByReadingDateDesc(Long accountId);
    List<MeterReading> findByBillingMonth(String billingMonth);
        @Query("SELECT COALESCE(SUM(m.unitsConsumed), 0) FROM MeterReading m WHERE m.readingDate BETWEEN :start AND :end")
        Long sumUnitsConsumedBetween(LocalDate start, LocalDate end);

        @Query("SELECT YEAR(m.readingDate) AS year, MONTH(m.readingDate) AS month, COALESCE(SUM(m.unitsConsumed), 0) AS units " +
            "FROM MeterReading m WHERE m.readingDate >= :since " +
            "GROUP BY YEAR(m.readingDate), MONTH(m.readingDate) ORDER BY year, month")
        List<MonthlyConsumptionProjection> findMonthlyConsumption(LocalDate since);
    @EntityGraph(attributePaths = {"account", "account.customer", "recordedBy"})
    Optional<MeterReading> findByReadingId(Long readingId);
    Optional<MeterReading> findByAccountAccountIdAndBillingMonth(Long accountId, String billingMonth);
    Optional<MeterReading> findFirstByAccountAccountIdOrderByReadingDateDesc(Long accountId);
}
