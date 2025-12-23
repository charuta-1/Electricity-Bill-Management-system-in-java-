package com.msedcl.billing.admin.reporting.dto.reporting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillStatusSummaryResponse {
    private long paid;
    private long unpaid;
    private long partiallyPaid;
    private long overdue;
}
