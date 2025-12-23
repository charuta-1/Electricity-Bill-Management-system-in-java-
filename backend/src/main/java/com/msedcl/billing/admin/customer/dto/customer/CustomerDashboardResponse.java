package com.msedcl.billing.admin.customer.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDashboardResponse {
    private CustomerProfileDto customer;
    private List<AccountSummaryDto> accounts;
    private BigDecimal totalOutstanding;
    private int openComplaints;
    private BillSummaryDto latestBill;
    private PaymentSummaryDto lastPayment;
    private List<BillSummaryDto> upcomingDueBills;
}
