package com.msedcl.billing.admin.tariff.repository;

import com.msedcl.billing.shared.entity.TariffSlab;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TariffSlabRepository extends JpaRepository<TariffSlab, Long> {
    List<TariffSlab> findByTariffMaster_TariffIdOrderBySlabNumberAsc(Long tariffId);
}
