package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSummaryDto {
    private Long accountId;
    private String accountNumber;
    private String meterNumber;
    private Account.ConnectionType connectionType;
    private String tariffCategory;
    private BigDecimal sanctionedLoad;
    private LocalDate connectionDate;
    private String installationAddress;
    private Boolean active;
    private BigDecimal outstandingBalance;
    private LocalDate lastBillDate;
    private LocalDate nextDueDate;
}
