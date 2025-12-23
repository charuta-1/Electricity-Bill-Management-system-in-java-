package com.msedcl.billing.shared.repository;

import com.msedcl.billing.shared.entity.AreaDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaDetailsRepository extends JpaRepository<AreaDetails, Long> {
    AreaDetails findByAreaName(String areaName);
}