package com.msedcl.billing.admin.tariff.dto.tariff;

import com.msedcl.billing.shared.entity.TariffMaster;
import com.msedcl.billing.shared.entity.TariffSlab;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TariffResponse(
    Long tariffId,
    String tariffCode,
    String tariffName,
    String connectionType,
    String description,
    BigDecimal fixedCharge,
    BigDecimal meterRent,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    Boolean isActive,
    List<TariffSlabResponse> slabs,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TariffResponse from(TariffMaster tariff) {
        List<TariffSlabResponse> slabResponses = tariff.getTariffSlabs() == null
            ? List.of()
            : tariff.getTariffSlabs().stream()
                .map(TariffSlabResponse::from)
                .toList();

        return new TariffResponse(
            tariff.getTariffId(),
            tariff.getTariffCode(),
            tariff.getTariffName(),
            tariff.getConnectionType() != null ? tariff.getConnectionType().name() : null,
            tariff.getDescription(),
            tariff.getFixedCharge(),
            tariff.getMeterRent(),
            tariff.getEffectiveFrom(),
            tariff.getEffectiveTo(),
            tariff.getIsActive(),
            slabResponses,
            tariff.getCreatedAt(),
            tariff.getUpdatedAt()
        );
    }

    public record TariffSlabResponse(
        Long slabId,
        Integer slabNumber,
        Integer minUnits,
        Integer maxUnits,
        BigDecimal ratePerUnit
    ) {
        private static TariffSlabResponse from(TariffSlab slab) {
            return new TariffSlabResponse(
                slab.getSlabId(),
                slab.getSlabNumber(),
                slab.getMinUnits(),
                slab.getMaxUnits(),
                slab.getRatePerUnit()
            );
        }
    }
}
