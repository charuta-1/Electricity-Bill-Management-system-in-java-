package com.msedcl.billing.admin.tariff.repository;

import com.msedcl.billing.shared.entity.AdditionalCharge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdditionalChargeRepository extends JpaRepository<AdditionalCharge, Long> {
    List<AdditionalCharge> findByIsActiveTrue();
}
