package com.msedcl.billing.admin.customer.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDetailsResponse {
    private AccountSummaryDto account;
    private List<BillSummaryDto> recentBills;
    private List<PaymentSummaryDto> recentPayments;
    private List<MeterReadingSummaryDto> recentReadings;
    private List<ComplaintSummaryDto> recentComplaints;
}
