package com.msedcl.billing.admin.account.dto.meter;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.MeterReading;
import com.msedcl.billing.shared.entity.User;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MeterReadingResponse(
    Long readingId,
    LocalDate readingDate,
    String billingMonth,
    Integer previousReading,
    Integer currentReading,
    Integer unitsConsumed,
    String readingType,
    String remarks,
    AccountSummary account,
    RecordedBySummary recordedBy,
    LocalDateTime createdAt
) {
    public static MeterReadingResponse from(MeterReading reading) {
        Account account = reading.getAccount();
        User recorder = reading.getRecordedBy();
        return new MeterReadingResponse(
            reading.getReadingId(),
            reading.getReadingDate(),
            reading.getBillingMonth(),
            reading.getPreviousReading(),
            reading.getCurrentReading(),
            reading.getUnitsConsumed(),
            reading.getReadingType() != null ? reading.getReadingType().name() : null,
            reading.getRemarks(),
            account != null ? AccountSummary.from(account) : null,
            recorder != null ? RecordedBySummary.from(recorder) : null,
            reading.getCreatedAt()
        );
    }

    public record AccountSummary(
        Long accountId,
        String accountNumber,
        String customerName,
        Long customerId
    ) {
        private static AccountSummary from(Account account) {
            String customerName = null;
            Long customerId = null;
            if (account.getCustomer() != null) {
                customerName = account.getCustomer().getFullName();
                customerId = account.getCustomer().getCustomerId();
            }
            return new AccountSummary(
                account.getAccountId(),
                account.getAccountNumber(),
                customerName,
                customerId
            );
        }
    }

    public record RecordedBySummary(
        Long userId,
        String username,
        String fullName
    ) {
        private static RecordedBySummary from(User user) {
            return new RecordedBySummary(
                user.getUserId(),
                user.getUsername(),
                user.getFullName()
            );
        }
    }
}
