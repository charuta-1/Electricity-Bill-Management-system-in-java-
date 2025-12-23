package com.msedcl.billing.admin.reporting.dto.reporting;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardMetricsResponse {
    private long totalCustomers;
    private long totalActiveAccounts;
    private long newCustomersThisMonth;
    private long newConnectionsThisMonth;
    private BigDecimal totalBilledThisMonth;
    private BigDecimal totalCollectedThisMonth;
    private BigDecimal totalOutstanding;
    private int unitsConsumedThisMonth;
    private long openComplaints;
    private long inProgressComplaints;
    private long resolvedToday;
    private BigDecimal collectionEfficiency;
    private long billsGeneratedThisMonth;
    private long overdueBills;
}
