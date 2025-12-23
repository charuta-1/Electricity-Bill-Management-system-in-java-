package com.msedcl.billing.admin.account.service;

import com.msedcl.billing.admin.account.dto.MeterReadingRequest;
import com.msedcl.billing.admin.account.dto.meter.MeterReadingResponse;
import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.MeterReading;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.account.repository.AccountRepository;
import com.msedcl.billing.admin.account.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeterReadingAdminService {

    private final MeterReadingRepository meterReadingRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public MeterReadingResponse addMeterReading(MeterReadingRequest request, User recordedBy) {
        if (request.getAccountId() == null) {
            throw new IllegalArgumentException("Account id is required to record a meter reading");
        }
        if (request.getCurrentReading() == null) {
            throw new IllegalArgumentException("Current reading value is required");
        }

        if (!StringUtils.hasText(request.getBillingMonth())) {
            throw new IllegalArgumentException("Billing month is required");
        }

        Account account = accountRepository.findByAccountId(request.getAccountId())
            .orElseThrow(() -> new RuntimeException("Account not found"));

        meterReadingRepository.findByAccountAccountIdAndBillingMonth(request.getAccountId(), request.getBillingMonth())
            .ifPresent(existing -> {
                throw new IllegalArgumentException(String.format(
                    "A meter reading for %s already exists on %s. Edit the existing reading or choose a different month.",
                    request.getBillingMonth(),
                    existing.getReadingDate()
                ));
            });

        MeterReading lastReading = meterReadingRepository
            .findFirstByAccountAccountIdOrderByReadingDateDesc(request.getAccountId())
            .orElse(null);

        int previousReading = lastReading != null ? lastReading.getCurrentReading() : 0;

        MeterReading reading = new MeterReading();
        reading.setAccount(account);
        reading.setReadingDate(LocalDate.now());
        reading.setBillingMonth(request.getBillingMonth());
        reading.setPreviousReading(previousReading);
        reading.setCurrentReading(request.getCurrentReading());
        reading.setReadingType(resolveReadingType(request.getReadingType()));
        reading.setRecordedBy(recordedBy);
        reading.setRemarks(request.getRemarks());

        MeterReading saved = meterReadingRepository.save(reading);

        return meterReadingRepository.findByReadingId(saved.getReadingId())
            .map(MeterReadingResponse::from)
            .orElseGet(() -> MeterReadingResponse.from(saved));
    }

    public List<MeterReadingResponse> getReadingsByAccount(Long accountId) {
        return meterReadingRepository.findByAccountAccountIdOrderByReadingDateDesc(accountId)
            .stream()
            .map(MeterReadingResponse::from)
            .toList();
    }

    public MeterReadingResponse getReading(Long id) {
        return meterReadingRepository.findByReadingId(id)
            .map(MeterReadingResponse::from)
            .orElseThrow(() -> new RuntimeException("Meter reading not found"));
    }

    private MeterReading.ReadingType resolveReadingType(String readingType) {
        if (readingType == null || readingType.isBlank()) {
            return MeterReading.ReadingType.ACTUAL;
        }
        try {
            return MeterReading.ReadingType.valueOf(readingType.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MeterReading.ReadingType.ACTUAL;
        }
    }
}
