package com.msedcl.billing.user.dashboard.service;

import com.msedcl.billing.admin.customer.dto.customer.*;
import com.msedcl.billing.shared.entity.*;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.admin.account.repository.AccountRepository;
import com.msedcl.billing.admin.billing.repository.BillRepository;
import com.msedcl.billing.user.payment.repository.PaymentRepository;
import com.msedcl.billing.admin.complaint.repository.ComplaintRepository;
import com.msedcl.billing.admin.account.repository.MeterReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerPortalService {

    private static final Collection<Bill.BillStatus> DUE_STATUSES = EnumSet.of(
        Bill.BillStatus.UNPAID,
        Bill.BillStatus.PARTIALLY_PAID,
        Bill.BillStatus.OVERDUE
    );

    private static final Collection<Complaint.Status> OPEN_COMPLAINT_STATUSES = EnumSet.of(
        Complaint.Status.OPEN,
        Complaint.Status.IN_PROGRESS
    );

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final BillRepository billRepository;
    private final PaymentRepository paymentRepository;
    private final ComplaintRepository complaintRepository;
    private final MeterReadingRepository meterReadingRepository;

    public CustomerDashboardResponse getDashboard(String username) {
        Customer customer = getCustomerForUser(username);
        List<Account> accounts = accountRepository.findByCustomerCustomerIdAndIsActiveTrue(customer.getCustomerId());

        List<AccountSummaryDto> accountSummaries = accounts.stream()
            .map(this::toAccountSummary)
            .collect(Collectors.toList());

        BigDecimal totalOutstanding = Optional.ofNullable(billRepository.sumOutstandingAmountByCustomer(customer.getCustomerId()))
            .orElse(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        int openComplaints = Math.toIntExact(
            complaintRepository.countByCustomerCustomerIdAndStatusIn(customer.getCustomerId(), OPEN_COMPLAINT_STATUSES)
        );

        BillSummaryDto latestBill = billRepository.findTopByAccountCustomerCustomerIdOrderByBillDateDesc(customer.getCustomerId())
            .map(this::toBillSummary)
            .orElse(null);

        PaymentSummaryDto lastPayment = paymentRepository.findTopByAccountCustomerCustomerIdOrderByPaymentDateDesc(customer.getCustomerId())
            .map(this::toPaymentSummary)
            .orElse(null);

        List<BillSummaryDto> upcomingDueBills = billRepository
            .findTop5ByAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(customer.getCustomerId(), DUE_STATUSES)
            .stream()
            .map(this::toBillSummary)
            .collect(Collectors.toList());

        return CustomerDashboardResponse.builder()
            .customer(toCustomerProfile(customer))
            .accounts(accountSummaries)
            .totalOutstanding(totalOutstanding)
            .openComplaints(openComplaints)
            .latestBill(latestBill)
            .lastPayment(lastPayment)
            .upcomingDueBills(upcomingDueBills)
            .build();
    }

    public List<AccountSummaryDto> getCustomerAccounts(String username) {
        Customer customer = getCustomerForUser(username);
        return accountRepository.findByCustomerCustomerId(customer.getCustomerId()).stream()
            .map(this::toAccountSummary)
            .collect(Collectors.toList());
    }

    public AccountDetailsResponse getAccountDetails(String username, Long accountId) {
        Customer customer = getCustomerForUser(username);
        Account account = getAccountForCustomer(customer, accountId);

        List<BillSummaryDto> bills = billRepository.findByAccountAccountIdOrderByBillDateDesc(account.getAccountId()).stream()
            .limit(12)
            .map(this::toBillSummary)
            .collect(Collectors.toList());

        List<PaymentSummaryDto> payments = paymentRepository.findByAccountAccountIdOrderByPaymentDateDesc(account.getAccountId()).stream()
            .limit(10)
            .map(this::toPaymentSummary)
            .collect(Collectors.toList());

        List<MeterReadingSummaryDto> readings = meterReadingRepository.findTop5ByAccountAccountIdOrderByReadingDateDesc(account.getAccountId()).stream()
            .map(this::toMeterReadingSummary)
            .collect(Collectors.toList());

        List<ComplaintSummaryDto> complaints = complaintRepository.findByAccountAccountIdOrderByCreatedAtDesc(account.getAccountId()).stream()
            .limit(5)
            .map(this::toComplaintSummary)
            .collect(Collectors.toList());

        return AccountDetailsResponse.builder()
            .account(toAccountSummary(account))
            .recentBills(bills)
            .recentPayments(payments)
            .recentReadings(readings)
            .recentComplaints(complaints)
            .build();
    }

    public List<BillSummaryDto> getAccountBills(String username, Long accountId) {
        Customer customer = getCustomerForUser(username);
        Account account = getAccountForCustomer(customer, accountId);
        return billRepository.findByAccountAccountIdOrderByBillDateDesc(account.getAccountId()).stream()
            .map(this::toBillSummary)
            .collect(Collectors.toList());
    }

    public BillDetailResponse getBillDetail(String username, Long billId) {
        Customer customer = getCustomerForUser(username);
        Bill bill = billRepository.findById(billId)
            .orElseThrow(() -> new RuntimeException("Bill not found with id: " + billId));

        if (!bill.getAccount().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new RuntimeException("Bill does not belong to the authenticated customer");
        }

        return toBillDetail(bill);
    }

    public List<PaymentSummaryDto> getAccountPayments(String username, Long accountId) {
        Customer customer = getCustomerForUser(username);
        Account account = getAccountForCustomer(customer, accountId);
        return paymentRepository.findByAccountAccountIdOrderByPaymentDateDesc(account.getAccountId()).stream()
            .map(this::toPaymentSummary)
            .collect(Collectors.toList());
    }

    public List<ComplaintSummaryDto> getAccountComplaints(String username, Long accountId) {
        Customer customer = getCustomerForUser(username);
        Account account = getAccountForCustomer(customer, accountId);
        return complaintRepository.findByAccountAccountIdOrderByCreatedAtDesc(account.getAccountId()).stream()
            .map(this::toComplaintSummary)
            .collect(Collectors.toList());
    }

    public List<MeterReadingSummaryDto> getAccountReadings(String username, Long accountId) {
        Customer customer = getCustomerForUser(username);
        Account account = getAccountForCustomer(customer, accountId);
        return meterReadingRepository.findByAccountAccountIdOrderByReadingDateDesc(account.getAccountId()).stream()
            .map(this::toMeterReadingSummary)
            .collect(Collectors.toList());
    }

    public List<ComplaintSummaryDto> getCustomerComplaints(String username) {
        Customer customer = getCustomerForUser(username);
        return complaintRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId()).stream()
            .map(this::toComplaintSummary)
            .collect(Collectors.toList());
    }

    public CustomerQuickSummary getQuickSummary(String username) {
        Customer customer = getCustomerForUser(username);

        BigDecimal outstanding = Optional.ofNullable(
            billRepository.sumOutstandingAmountByCustomer(customer.getCustomerId()))
            .orElse(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        Bill latestBill = billRepository
            .findTopByAccountCustomerCustomerIdOrderByBillDateDesc(customer.getCustomerId())
            .orElse(null);

        BigDecimal lastBillAmount = latestBill != null && latestBill.getNetPayable() != null
            ? latestBill.getNetPayable().setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        List<Bill> recentBills = billRepository
            .findTop6ByAccountCustomerCustomerIdOrderByBillDateDesc(customer.getCustomerId());

        BigDecimal averageConsumption = recentBills.stream()
            .map(Bill::getUnitsConsumed)
            .filter(units -> units != null && units > 0)
            .map(BigDecimal::valueOf)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = recentBills.stream()
            .map(Bill::getUnitsConsumed)
            .filter(units -> units != null && units > 0)
            .count();

        BigDecimal avgValue = count == 0
            ? BigDecimal.ZERO
            : averageConsumption.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        Bill upcomingDue = billRepository
            .findTopByAccountCustomerCustomerIdAndBillStatusInOrderByDueDateAsc(
                customer.getCustomerId(), DUE_STATUSES)
            .orElse(null);

        LocalDate nextDueDate = upcomingDue != null ? upcomingDue.getDueDate() : null;

        return new CustomerQuickSummary(outstanding, lastBillAmount, avgValue, nextDueDate);
    }

    public List<CustomerBillListItem> getAllBills(String username) {
        Customer customer = getCustomerForUser(username);
        return billRepository.findByAccountCustomerCustomerIdOrderByBillDateDesc(customer.getCustomerId()).stream()
            .map(this::toCustomerBillListItem)
            .collect(Collectors.toList());
    }

    public List<CustomerBillListItem> getPendingBills(String username) {
        Customer customer = getCustomerForUser(username);
        return billRepository
            .findByAccountCustomerCustomerIdAndBillStatusIn(customer.getCustomerId(), DUE_STATUSES)
            .stream()
            .map(this::toCustomerBillListItem)
            .collect(Collectors.toList());
    }

    public List<ConsumptionPointDto> getConsumptionTrend(String username, int months) {
        Customer customer = getCustomerForUser(username);
        List<Bill> bills = billRepository.findByAccountCustomerCustomerIdOrderByBillDateDesc(customer.getCustomerId());

        TreeMap<String, Long> unitsByMonth = new TreeMap<>();
        bills.stream()
            .filter(bill -> bill.getBillMonth() != null)
            .forEach(bill -> unitsByMonth.merge(
                bill.getBillMonth(),
                Optional.ofNullable(bill.getUnitsConsumed()).map(Integer::longValue).orElse(0L),
                Long::sum));

        return unitsByMonth.entrySet().stream()
            .skip(Math.max(0, unitsByMonth.size() - months))
            .map(entry -> new ConsumptionPointDto(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    }

    public List<ComplaintListItemDto> getCustomerComplaintFeed(String username) {
        Customer customer = getCustomerForUser(username);
        return complaintRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customer.getCustomerId()).stream()
            .map(complaint -> new ComplaintListItemDto(
                complaint.getComplaintId(),
                complaint.getComplaintNumber(),
                complaint.getComplaintType(),
                complaint.getPriority(),
                complaint.getStatus(),
                complaint.getSubject(),
                complaint.getUpdatedAt()
            ))
            .collect(Collectors.toList());
    }

    public CustomerProfileDto getCustomerProfile(String username) {
        Customer customer = getCustomerForUser(username);
        return toCustomerProfileWithAreaDetails(customer);
    }

    private Customer getCustomerForUser(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

        return customerRepository.findByUserUserId(user.getUserId())
            .orElseThrow(() -> new RuntimeException("No customer profile mapped to user: " + user.getUsername()));
    }

    private Account getAccountForCustomer(Customer customer, Long accountId) {
        return accountRepository.findById(accountId)
            .filter(account -> account.getCustomer().getCustomerId().equals(customer.getCustomerId()))
            .orElseThrow(() -> new RuntimeException("Account not found for customer"));
    }

    private CustomerProfileDto toCustomerProfile(Customer customer) {
        return CustomerProfileDto.builder()
            .customerId(customer.getCustomerId())
            .customerNumber(customer.getCustomerNumber())
            .fullName(customer.getFullName())
            .email(customer.getEmail())
            .phoneNumber(customer.getPhoneNumber())
            .address(customer.getAddress())
            .city(customer.getCity())
            .state(customer.getState())
            .pincode(customer.getPincode())
            .build();
    }

    private CustomerProfileDto toCustomerProfileWithAreaDetails(Customer customer) {
        var areaDetails = customer.getAreaDetails();
        return CustomerProfileDto.builder()
            .customerId(customer.getCustomerId())
            .customerNumber(customer.getCustomerNumber())
            .fullName(customer.getFullName())
            .email(customer.getEmail())
            .phoneNumber(customer.getPhoneNumber())
            .address(customer.getAddress())
            .city(customer.getCity())
            .state(customer.getState())
            .pincode(customer.getPincode())
            .areaId(areaDetails != null ? areaDetails.getId() : null)
            .areaName(areaDetails != null ? areaDetails.getAreaName() : null)
            .transformerNo(areaDetails != null ? areaDetails.getTransformerNo() : null)
            .feederNo(areaDetails != null ? areaDetails.getFeederNo() : null)
            .poleNo(areaDetails != null ? areaDetails.getPoleNo() : null)
            .advancePayment(customer.getAdvancePayment())
            .build();
    }

    private AccountSummaryDto toAccountSummary(Account account) {
        BigDecimal outstanding = Optional.ofNullable(billRepository.sumOutstandingAmountByAccount(account.getAccountId()))
            .orElse(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP);

        LocalDateWrapper billDates = billRepository.findTopByAccountAccountIdOrderByBillDateDesc(account.getAccountId())
            .map(bill -> new LocalDateWrapper(bill.getBillDate(), bill.getDueDate()))
            .orElse(null);

        return AccountSummaryDto.builder()
            .accountId(account.getAccountId())
            .accountNumber(account.getAccountNumber())
            .meterNumber(account.getMeterNumber())
            .connectionType(account.getConnectionType())
            .tariffCategory(account.getTariffCategory())
            .sanctionedLoad(account.getSanctionedLoad())
            .connectionDate(account.getConnectionDate())
            .installationAddress(account.getInstallationAddress())
            .active(Boolean.TRUE.equals(account.getIsActive()))
            .outstandingBalance(outstanding)
            .lastBillDate(billDates != null ? billDates.billDate : null)
            .nextDueDate(billDates != null ? billDates.dueDate : null)
            .build();
    }

    private BillSummaryDto toBillSummary(Bill bill) {
        return BillSummaryDto.builder()
            .billId(bill.getBillId())
            .accountId(bill.getAccount().getAccountId())
            .invoiceNumber(bill.getInvoiceNumber())
            .billMonth(bill.getBillMonth())
            .billDate(bill.getBillDate())
            .dueDate(bill.getDueDate())
            .unitsConsumed(bill.getUnitsConsumed())
            .netPayable(bill.getNetPayable())
            .amountPaid(bill.getAmountPaid())
            .balanceAmount(bill.getBalanceAmount())
            .status(bill.getBillStatus())
            .build();
    }

    private BillDetailResponse toBillDetail(Bill bill) {
        return BillDetailResponse.builder()
            .billId(bill.getBillId())
            .accountId(bill.getAccount().getAccountId())
            .invoiceNumber(bill.getInvoiceNumber())
            .billMonth(bill.getBillMonth())
            .billDate(bill.getBillDate())
            .dueDate(bill.getDueDate())
            .unitsConsumed(bill.getUnitsConsumed())
            .energyCharges(bill.getEnergyCharges())
            .fixedCharges(bill.getFixedCharges())
            .meterRent(bill.getMeterRent())
            .electricityDuty(bill.getElectricityDuty())
            .otherCharges(bill.getOtherCharges())
            .subsidyAmount(bill.getSubsidyAmount())
            .lateFee(bill.getLateFee())
            .totalAmount(bill.getTotalAmount())
            .previousDue(bill.getPreviousDue())
            .netPayable(bill.getNetPayable())
            .amountPaid(bill.getAmountPaid())
            .balanceAmount(bill.getBalanceAmount())
            .status(bill.getBillStatus())
            .pdfPath(bill.getPdfPath())
            .qrCodePath(bill.getQrCodePath())
            .build();
    }

    private PaymentSummaryDto toPaymentSummary(Payment payment) {
        return PaymentSummaryDto.builder()
            .paymentId(payment.getPaymentId())
            .billId(payment.getBill().getBillId())
            .accountId(payment.getAccount().getAccountId())
            .paymentReference(payment.getPaymentReference())
            .paymentDate(payment.getPaymentDate())
            .paymentAmount(payment.getPaymentAmount())
            .convenienceFee(payment.getConvenienceFee())
            .netAmount(payment.getNetAmount())
            .paymentMode(payment.getPaymentMode())
            .paymentStatus(payment.getPaymentStatus())
            .paymentChannel(payment.getPaymentChannel())
            .build();
    }

    private ComplaintSummaryDto toComplaintSummary(Complaint complaint) {
        return ComplaintSummaryDto.builder()
            .complaintId(complaint.getComplaintId())
            .accountId(complaint.getAccount() != null ? complaint.getAccount().getAccountId() : null)
            .complaintNumber(complaint.getComplaintNumber())
            .complaintType(complaint.getComplaintType())
            .priority(complaint.getPriority())
            .status(complaint.getStatus())
            .subject(complaint.getSubject())
            .description(complaint.getDescription())
            .createdAt(complaint.getCreatedAt())
            .build();
    }

    private MeterReadingSummaryDto toMeterReadingSummary(MeterReading reading) {
        return MeterReadingSummaryDto.builder()
            .readingId(reading.getReadingId())
            .accountId(reading.getAccount().getAccountId())
            .billingMonth(reading.getBillingMonth())
            .readingDate(reading.getReadingDate())
            .previousReading(reading.getPreviousReading())
            .currentReading(reading.getCurrentReading())
            .unitsConsumed(reading.getUnitsConsumed())
            .readingType(reading.getReadingType())
            .build();
    }

    private record LocalDateWrapper(java.time.LocalDate billDate, java.time.LocalDate dueDate) { }

    private CustomerBillListItem toCustomerBillListItem(Bill bill) {
        return new CustomerBillListItem(
            bill.getBillId(),
            bill.getInvoiceNumber(),
            bill.getBillDate(),
            bill.getDueDate(),
            bill.getUnitsConsumed(),
            bill.getNetPayable(),
            Optional.ofNullable(bill.getBalanceAmount()).orElse(bill.getNetPayable()),
            bill.getBillStatus(),
            bill.getPdfPath(),
            bill.getQrCodePath()
        );
    }
}
