package com.msedcl.billing.admin.tariff.repository;

import com.msedcl.billing.shared.entity.TariffMaster;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TariffMasterRepository extends JpaRepository<TariffMaster, Long> {
    Optional<TariffMaster> findByTariffCode(String tariffCode);
    List<TariffMaster> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"tariffSlabs"})
    List<TariffMaster> findAllByOrderByTariffCodeAsc();

    @EntityGraph(attributePaths = {"tariffSlabs"})
    Optional<TariffMaster> findByTariffId(Long tariffId);

    @Query("SELECT t FROM TariffMaster t WHERE t.tariffCode = :tariffCode " +
           "AND t.effectiveFrom <= :date " +
           "AND (t.effectiveTo IS NULL OR t.effectiveTo >= :date)")
    Optional<TariffMaster> findActiveTariffByCodeAndDate(String tariffCode, LocalDate date);
}
