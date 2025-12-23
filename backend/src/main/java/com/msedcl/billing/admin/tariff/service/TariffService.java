package com.msedcl.billing.admin.tariff.service;

import com.msedcl.billing.admin.tariff.dto.tariff.TariffResponse;
import com.msedcl.billing.shared.entity.TariffMaster;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.tariff.repository.TariffMasterRepository;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TariffService {

    private final TariffMasterRepository tariffMasterRepository;
    private final AuditLogService auditLogService;

    public List<TariffResponse> getAllTariffsForAdmin() {
        return tariffMasterRepository.findAllByOrderByTariffCodeAsc()
            .stream()
            .map(TariffResponse::from)
            .toList();
    }

    public TariffMaster getTariffById(Long id) {
        return tariffMasterRepository.findByTariffId(id)
            .orElseThrow(() -> new RuntimeException("Tariff not found with id: " + id));
    }

    public TariffResponse getTariffResponseById(Long id) {
        return TariffResponse.from(getTariffById(id));
    }

    @Transactional
    public TariffMaster createTariff(TariffMaster tariffMaster, User actor, String ipAddress) {
        TariffMaster savedTariff = tariffMasterRepository.save(tariffMaster);

        auditLogService.record(actor,
            "CREATE_TARIFF",
            "TariffMaster",
            savedTariff.getTariffId(),
            String.format("Created tariff %s (%s)", savedTariff.getTariffName(), savedTariff.getTariffCode()),
            ipAddress);

        return savedTariff;
    }

    @Transactional
    public TariffMaster updateTariff(Long id, TariffMaster tariffDetails, User actor, String ipAddress) {
        TariffMaster tariff = getTariffById(id);

        tariff.setTariffCode(tariffDetails.getTariffCode());
        tariff.setTariffName(tariffDetails.getTariffName());
        tariff.setFixedCharge(tariffDetails.getFixedCharge());
        tariff.setMeterRent(tariffDetails.getMeterRent());
        tariff.setEffectiveFrom(tariffDetails.getEffectiveFrom());
        tariff.setEffectiveTo(tariffDetails.getEffectiveTo());
        tariff.setIsActive(tariffDetails.getIsActive());

        TariffMaster updatedTariff = tariffMasterRepository.save(tariff);

        auditLogService.record(actor,
            "UPDATE_TARIFF",
            "TariffMaster",
            updatedTariff.getTariffId(),
            String.format("Updated tariff %s (%s)", updatedTariff.getTariffName(), updatedTariff.getTariffCode()),
            ipAddress);

        return updatedTariff;
    }
}
