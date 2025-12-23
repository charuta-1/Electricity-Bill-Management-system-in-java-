package com.msedcl.billing.admin.customer.dto.customer;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerQuickSummary(
    BigDecimal outstandingAmount,
    BigDecimal lastBillAmount,
    BigDecimal averageConsumption,
    LocalDate nextDueDate
) {
}
