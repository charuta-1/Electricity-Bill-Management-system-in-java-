package com.msedcl.billing.admin.reporting.service;

import com.msedcl.billing.admin.reporting.dto.reporting.*;
import com.msedcl.billing.shared.entity.Bill;
import com.msedcl.billing.shared.entity.Complaint;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.admin.account.repository.AccountRepository;
import com.msedcl.billing.admin.billing.repository.BillRepository;
import com.msedcl.billing.user.payment.repository.PaymentRepository;
import com.msedcl.billing.admin.complaint.repository.ComplaintRepository;
import com.msedcl.billing.admin.account.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportingService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final ComplaintRepository complaintRepository;
    private final MeterReadingRepository meterReadingRepository;

    public DashboardMetricsResponse getDashboardMetrics() {
        LocalDate today = LocalDate.now();
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        LocalDateTime startOfMonth = firstOfMonth.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(LocalTime.MAX);

        BigDecimal totalBilledThisMonth = Optional.ofNullable(billRepository.sumBilledBetween(firstOfMonth, today)).orElse(BigDecimal.ZERO);
        BigDecimal totalCollectedThisMonth = Optional.ofNullable(paymentRepository.sumSuccessfulPaymentsBetween(startOfMonth, endOfToday)).orElse(BigDecimal.ZERO);
        BigDecimal totalOutstanding = Optional.ofNullable(billRepository.sumOutstandingAmount()).orElse(BigDecimal.ZERO);
        long unitsConsumedThisMonth = Optional.ofNullable(meterReadingRepository.sumUnitsConsumedBetween(firstOfMonth, today)).orElse(0L);

        long totalCustomers = customerRepository.count();
        long totalActiveAccounts = accountRepository.countByIsActiveTrue();
        long newCustomersThisMonth = customerRepository.countByCreatedAtBetween(startOfMonth, endOfToday);
        long newConnectionsThisMonth = accountRepository.countByCreatedAtBetween(startOfMonth, endOfToday);
        long billsGeneratedThisMonth = billRepository.countByBillDateBetween(firstOfMonth, today);

        long openComplaints = complaintRepository.countByStatus(Complaint.Status.OPEN);
        long inProgressComplaints = complaintRepository.countByStatus(Complaint.Status.IN_PROGRESS);
        long resolvedComplaints = complaintRepository.countByStatus(Complaint.Status.RESOLVED);
        long overdueBills = billRepository.countByBillStatus(Bill.BillStatus.OVERDUE);

        BigDecimal collectionEfficiency = BigDecimal.ZERO;
        if (totalBilledThisMonth.compareTo(BigDecimal.ZERO) > 0) {
            collectionEfficiency = totalCollectedThisMonth
                .divide(totalBilledThisMonth, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
        }

        return DashboardMetricsResponse.builder()
            .totalCustomers(totalCustomers)
            .totalActiveAccounts(totalActiveAccounts)
            .newCustomersThisMonth(newCustomersThisMonth)
            .newConnectionsThisMonth(newConnectionsThisMonth)
            .totalBilledThisMonth(totalBilledThisMonth.setScale(2, RoundingMode.HALF_UP))
            .totalCollectedThisMonth(totalCollectedThisMonth.setScale(2, RoundingMode.HALF_UP))
            .totalOutstanding(totalOutstanding.setScale(2, RoundingMode.HALF_UP))
            .unitsConsumedThisMonth(Math.toIntExact(unitsConsumedThisMonth))
            .openComplaints(openComplaints)
            .inProgressComplaints(inProgressComplaints)
            .resolvedToday(resolvedComplaints)
            .collectionEfficiency(collectionEfficiency)
        .billsGeneratedThisMonth(billsGeneratedThisMonth)
        .overdueBills(overdueBills)
            .build();
    }

    public BillStatusSummaryResponse getBillStatusSummary() {
    long paid = billRepository.countByBillStatus(Bill.BillStatus.PAID);
    long unpaid = billRepository.countByBillStatus(Bill.BillStatus.UNPAID);
    long overdue = billRepository.countByBillStatus(Bill.BillStatus.OVERDUE);
    long partiallyPaid = billRepository.countByBillStatus(Bill.BillStatus.PARTIALLY_PAID);

    return BillStatusSummaryResponse.builder()
        .paid(paid)
        .unpaid(unpaid)
        .partiallyPaid(partiallyPaid)
        .overdue(overdue)
        .build();
    }

    public List<MonthlyAmountResponse> getCollectionTrend(int months) {
        LocalDateTime since = LocalDate.now().minusMonths(months - 1L).withDayOfMonth(1).atStartOfDay();
        return paymentRepository.findMonthlyCollections(since).stream()
            .map(projection -> new MonthlyAmountResponse(
                projection.getYear(),
                projection.getMonth(),
                Optional.ofNullable(projection.getTotalAmount()).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)))
            .collect(Collectors.toList());
    }

    public List<MonthlyConsumptionResponse> getConsumptionTrend(int months) {
        LocalDate since = LocalDate.now().minusMonths(months - 1L).withDayOfMonth(1);
        return meterReadingRepository.findMonthlyConsumption(since).stream()
            .map(projection -> new MonthlyConsumptionResponse(
                projection.getYear(),
                projection.getMonth(),
                Optional.ofNullable(projection.getUnits()).orElse(0L).intValue()))
            .collect(Collectors.toList());
    }
}
