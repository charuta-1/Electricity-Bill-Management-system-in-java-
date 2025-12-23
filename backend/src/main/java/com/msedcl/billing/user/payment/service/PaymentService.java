package com.msedcl.billing.user.payment.service;

import com.msedcl.billing.user.payment.dto.PaymentRequest;
import com.msedcl.billing.shared.entity.AdditionalCharge;
import com.msedcl.billing.shared.entity.Bill;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.Payment;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.tariff.repository.AdditionalChargeRepository;
import com.msedcl.billing.admin.billing.repository.BillRepository;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.user.payment.repository.PaymentRepository;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import com.msedcl.billing.shared.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final DateTimeFormatter CHEQUE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final PaymentRepository paymentRepository;
    private final BillRepository billRepository;
    private final AdditionalChargeRepository additionalChargeRepository;
    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    @Transactional
    public Payment recordPayment(PaymentRequest request, User processedBy, String ipAddress) {
        Bill bill = billRepository.findById(request.getBillId())
            .orElseThrow(() -> new RuntimeException("Bill not found with id: " + request.getBillId()));

        // First: apply any available advance (customer wallet) to reduce outstanding balance
        BigDecimal outstandingBefore = Optional.ofNullable(bill.getBalanceAmount()).orElse(bill.getNetPayable());
        Customer customer = bill.getAccount().getCustomer();

        BigDecimal advanceAvailable = BigDecimal.ZERO;
        if (customer.getAdvancePayment() != null && customer.getAdvancePayment() > 0) {
            advanceAvailable = BigDecimal.valueOf(customer.getAdvancePayment()).setScale(2, RoundingMode.HALF_UP);
        }

        Payment advanceAdjustmentPayment = null;
        if (advanceAvailable.compareTo(BigDecimal.ZERO) > 0 && outstandingBefore.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal applyFromAdvance = advanceAvailable.min(outstandingBefore).setScale(2, RoundingMode.HALF_UP);

            // deduct from customer wallet
            double newAdvance = customer.getAdvancePayment() - applyFromAdvance.doubleValue();
            customer.setAdvancePayment(newAdvance);
            customerRepository.save(customer);

            // update bill amounts/status
            BigDecimal safeAmountPaid = Optional.ofNullable(bill.getAmountPaid()).orElse(BigDecimal.ZERO);
            BigDecimal newAmountPaid = safeAmountPaid.add(applyFromAdvance).setScale(2, RoundingMode.HALF_UP);
            BigDecimal newBalance = Optional.ofNullable(bill.getBalanceAmount()).orElse(bill.getNetPayable()).subtract(applyFromAdvance).setScale(2, RoundingMode.HALF_UP);

            bill.setAmountPaid(newAmountPaid);
            bill.setBalanceAmount(newBalance.max(BigDecimal.ZERO));
            if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
                bill.setBillStatus(Bill.BillStatus.PAID);
            } else {
                bill.setBillStatus(Bill.BillStatus.PARTIALLY_PAID);
            }

            // Persist a payment record representing the advance application so there's a trace
            Payment adj = new Payment();
            adj.setBill(bill);
            adj.setAccount(bill.getAccount());
            adj.setPaymentAmount(applyFromAdvance);
            adj.setConvenienceFee(BigDecimal.ZERO);
            adj.setNetAmount(applyFromAdvance);
            adj.setPaymentMode(Payment.PaymentMode.CASH); // using CASH as an internal marker for adjustment
            adj.setPaymentChannel("ADVANCE_ADJUSTMENT");
            adj.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
            adj.setPaymentDate(LocalDateTime.now());
            adj.setTransactionId("ADVANCE-ADJ-" + generatePaymentReference());
            adj.setPaymentReference(generatePaymentReference());
            adj.setProcessedBy(processedBy);

            advanceAdjustmentPayment = paymentRepository.save(adj);
            billRepository.save(bill);

            auditLogService.record(processedBy,
                "APPLY_ADVANCE",
                "Bill",
                bill.getBillId(),
                "Applied advance payment of ₹" + applyFromAdvance + " from customer wallet to invoice " + bill.getInvoiceNumber(),
                ipAddress);
        }

        // Recompute outstanding after applying advance
        BigDecimal outstanding = Optional.ofNullable(bill.getBalanceAmount()).orElse(bill.getNetPayable());

        // Read and validate requested payment amount (if any)
        BigDecimal paymentAmount = Optional.ofNullable(request.getPaymentAmount()).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        if (paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
            // If no external payment requested but advance fully covered bill, return the adjustment payment
            if (outstanding.compareTo(BigDecimal.ZERO) == 0 && advanceAdjustmentPayment != null) {
                // send notification for the adjustment
                try { notificationService.sendPaymentReceiptEmail(advanceAdjustmentPayment); } catch (Exception ignored) {}
                return advanceAdjustmentPayment;
            }
            throw new IllegalArgumentException("Payment amount must be greater than zero");
        }

        if (outstanding != null && paymentAmount.compareTo(outstanding.setScale(2, RoundingMode.HALF_UP)) > 0) {
            throw new IllegalArgumentException("Payment amount cannot exceed outstanding balance of ₹" + outstanding.setScale(2, RoundingMode.HALF_UP));
        }

        Payment.PaymentMode paymentMode;
        try {
            paymentMode = Payment.PaymentMode.valueOf(request.getPaymentMode().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unsupported payment mode: " + request.getPaymentMode());
        }

        BigDecimal convenienceFee = calculateConvenienceFee(paymentMode, paymentAmount, bill.getAccount().getTariffCategory());
        BigDecimal netAmount = paymentAmount.add(convenienceFee).setScale(2, RoundingMode.HALF_UP);

        Payment payment = new Payment();
        payment.setBill(bill);
        payment.setAccount(bill.getAccount());
        payment.setPaymentAmount(paymentAmount);
        payment.setConvenienceFee(convenienceFee);
        payment.setNetAmount(netAmount);
        payment.setPaymentMode(paymentMode);
        payment.setPaymentChannel(Optional.ofNullable(request.getPaymentChannel()).filter(StringUtils::hasText).orElse(paymentMode.name()));
        payment.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());
        payment.setTransactionId(resolveTransactionId(request));
        payment.setUpiReference(resolveUpiReference(request, paymentMode));
        payment.setChequeNumber(request.getChequeNumber());
        payment.setChequeDate(resolveChequeDate(request.getChequeDate()));
        payment.setBankName(request.getBankName());
        payment.setRemarks(request.getRemarks());
        payment.setPaymentReference(generatePaymentReference());
        payment.setProcessedBy(processedBy);

        updateBillSettlement(bill, paymentAmount);

        Payment savedPayment = paymentRepository.save(payment);
        billRepository.save(bill);

        auditLogService.record(processedBy,
            "RECORD_PAYMENT",
            "Payment",
            savedPayment.getPaymentId(),
            "Recorded payment " + savedPayment.getPaymentReference() + " for invoice " + bill.getInvoiceNumber(),
            ipAddress);

        notificationService.sendPaymentReceiptEmail(savedPayment);

        return savedPayment;
    }

    private void updateBillSettlement(Bill bill, BigDecimal paymentAmount) {
        BigDecimal safeAmountPaid = Optional.ofNullable(bill.getAmountPaid()).orElse(BigDecimal.ZERO);
        BigDecimal safeBalance = Optional.ofNullable(bill.getBalanceAmount()).orElse(bill.getNetPayable());

        BigDecimal newAmountPaid = safeAmountPaid.add(paymentAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newBalance = safeBalance.subtract(paymentAmount).setScale(2, RoundingMode.HALF_UP);

        bill.setAmountPaid(newAmountPaid);
        bill.setBalanceAmount(newBalance.max(BigDecimal.ZERO));

        if (newBalance.compareTo(BigDecimal.ZERO) <= 0) {
            bill.setBillStatus(Bill.BillStatus.PAID);
        } else {
            bill.setBillStatus(Bill.BillStatus.PARTIALLY_PAID);
        }
    }

    private BigDecimal calculateConvenienceFee(Payment.PaymentMode paymentMode, BigDecimal paymentAmount, String tariffCategory) {
        String chargeName = "Convenience Fee " + paymentMode.name();
        BigDecimal fee = additionalChargeRepository.findByIsActiveTrue().stream()
            .filter(charge -> charge.getChargeName().equalsIgnoreCase(chargeName))
            .filter(charge -> {
                String applicable = charge.getApplicableTo();
                return "ALL".equalsIgnoreCase(applicable) || applicable.contains(tariffCategory);
            })
            .map(charge -> resolveChargeAmount(charge, paymentAmount))
            .findFirst()
            .orElse(BigDecimal.ZERO);
        return fee.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resolveChargeAmount(AdditionalCharge charge, BigDecimal baseAmount) {
        if (charge.getChargeType() == AdditionalCharge.ChargeType.PERCENTAGE) {
            return baseAmount.multiply(charge.getChargeValue())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        return Optional.ofNullable(charge.getChargeValue()).orElse(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveTransactionId(PaymentRequest request) {
        if (StringUtils.hasText(request.getTransactionId())) {
            return request.getTransactionId();
        }
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String resolveUpiReference(PaymentRequest request, Payment.PaymentMode mode) {
        if (mode != Payment.PaymentMode.UPI) {
            return request.getUpiReference();
        }
        if (StringUtils.hasText(request.getUpiReference())) {
            return request.getUpiReference();
        }
        return "UPI-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase(Locale.ROOT);
    }

    private LocalDate resolveChequeDate(String chequeDate) {
        if (!StringUtils.hasText(chequeDate)) {
            return null;
        }
        return LocalDate.parse(chequeDate, CHEQUE_DATE_FORMAT);
    }

    private String generatePaymentReference() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    @Transactional
    public void addAdvancePayment(Double amount, User user, String ipAddress) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Advance payment amount must be greater than zero");
        }

        Customer customer = customerRepository.findByUserUserId(user.getUserId())
            .orElseThrow(() -> new RuntimeException("Customer profile not found"));

        Double currentAdvance = customer.getAdvancePayment() != null ? customer.getAdvancePayment() : 0.00;
        customer.setAdvancePayment(currentAdvance + amount);
        customerRepository.save(customer);

        auditLogService.record(user,
            "ADD_ADVANCE_PAYMENT",
            "Customer",
            customer.getCustomerId(),
            String.format("Added advance payment of ₹%.2f. New balance: ₹%.2f", amount, customer.getAdvancePayment()),
            ipAddress);
    }

    @Transactional
    public void addAdvancePaymentForCustomer(Long customerId, Double amount, User processedBy, String ipAddress) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Advance payment amount must be greater than zero");
        }

        com.msedcl.billing.shared.entity.Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new RuntimeException("Customer not found with id: " + customerId));

        Double currentAdvance = customer.getAdvancePayment() != null ? customer.getAdvancePayment() : 0.00;
        customer.setAdvancePayment(currentAdvance + amount);
        customerRepository.save(customer);

        auditLogService.record(processedBy,
            "ADMIN_ADD_ADVANCE",
            "Customer",
            customer.getCustomerId(),
            String.format("Admin %s added advance payment of ₹%.2f to customer %s. New balance: ₹%.2f", processedBy.getUsername(), amount, customer.getCustomerNumber(), customer.getAdvancePayment()),
            ipAddress);
    }

    @Transactional(readOnly = true)
    public Double getAdvancePayment(User user) {
        Customer customer = customerRepository.findByUserUserId(user.getUserId())
            .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        
        return customer.getAdvancePayment() != null ? customer.getAdvancePayment() : 0.00;
    }
}
