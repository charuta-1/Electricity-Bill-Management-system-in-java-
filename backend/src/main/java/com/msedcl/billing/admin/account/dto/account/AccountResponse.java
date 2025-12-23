package com.msedcl.billing.admin.account.dto.account;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AccountResponse(
    Long accountId,
    String accountNumber,
    String meterNumber,
    String connectionType,
    BigDecimal sanctionedLoad,
    LocalDate connectionDate,
    String installationAddress,
    String tariffCategory,
    Boolean isActive,
    CustomerSummary customer,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AccountResponse from(Account account) {
        Customer customer = account.getCustomer();
        return new AccountResponse(
            account.getAccountId(),
            account.getAccountNumber(),
            account.getMeterNumber(),
            account.getConnectionType() != null ? account.getConnectionType().name() : null,
            account.getSanctionedLoad(),
            account.getConnectionDate(),
            account.getInstallationAddress(),
            account.getTariffCategory(),
            account.getIsActive(),
            customer != null ? CustomerSummary.from(customer) : null,
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }

    public record CustomerSummary(
        Long customerId,
        String customerNumber,
        String fullName,
        String email,
        String phoneNumber,
        String username
    ) {
        private static CustomerSummary from(Customer customer) {
            User user = customer.getUser();
            return new CustomerSummary(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getFullName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                user != null ? user.getUsername() : null
            );
        }
    }
}
