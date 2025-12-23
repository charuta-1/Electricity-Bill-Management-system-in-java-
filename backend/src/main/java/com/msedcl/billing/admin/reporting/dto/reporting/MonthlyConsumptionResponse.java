package com.msedcl.billing.admin.reporting.dto.reporting;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MonthlyConsumptionResponse {
    private int year;
    private int month;
    private int units;
}
