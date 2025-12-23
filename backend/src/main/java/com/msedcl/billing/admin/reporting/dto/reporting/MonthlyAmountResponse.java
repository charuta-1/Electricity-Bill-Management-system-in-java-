package com.msedcl.billing.admin.reporting.dto.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class MonthlyAmountResponse {
    private int year;
    private int month;
    private BigDecimal totalAmount;
}
