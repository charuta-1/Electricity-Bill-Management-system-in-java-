package com.msedcl.billing.admin.billing.service;

import com.msedcl.billing.admin.billing.dto.BillBatchGenerationResponse;
import com.msedcl.billing.shared.entity.*;
import com.msedcl.billing.admin.billing.repository.BillRepository;
import com.msedcl.billing.admin.account.repository.MeterReadingRepository;
import com.msedcl.billing.admin.tariff.repository.TariffMasterRepository;
import com.msedcl.billing.admin.tariff.repository.TariffSlabRepository;
import com.msedcl.billing.admin.tariff.repository.AdditionalChargeRepository;
import com.msedcl.billing.admin.tariff.repository.SubsidyRuleRepository;
import com.msedcl.billing.admin.tariff.repository.LateFeePolicyRepository;
import com.msedcl.billing.shared.service.PdfService;
import com.msedcl.billing.shared.service.QrCodeService;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import com.msedcl.billing.shared.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final BillRepository billRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final TariffMasterRepository tariffMasterRepository;
    private final TariffSlabRepository tariffSlabRepository;
    private final AdditionalChargeRepository additionalChargeRepository;
    private final SubsidyRuleRepository subsidyRuleRepository;
    private final LateFeePolicyRepository lateFeePolicyRepository;
    private final PdfService pdfService;
    private final QrCodeService qrCodeService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final com.msedcl.billing.admin.customer.repository.CustomerRepository customerRepository;
    private final com.msedcl.billing.user.payment.repository.PaymentRepository paymentRepository;

    @Transactional
    public Bill generateBill(Long readingId, User generatedBy) {
        MeterReading reading = meterReadingRepository.findById(readingId)
            .orElseThrow(() -> new RuntimeException("Reading not found"));

        Account account = reading.getAccount();

        billRepository.findByAccountAccountIdAndBillMonth(account.getAccountId(), reading.getBillingMonth())
            .ifPresent(existing -> {
                throw new RuntimeException("Bill already generated for billing month: " + reading.getBillingMonth());
            });

        LocalDate billDate = LocalDate.now();

        TariffMaster tariff = tariffMasterRepository
            .findActiveTariffByCodeAndDate(account.getTariffCategory(), billDate)
            .orElseThrow(() -> new RuntimeException("Tariff not found"));

        int unitsConsumed = resolveUnitsConsumed(reading);

        BigDecimal energyCharges = calculateEnergyCharges(tariff.getTariffId(), unitsConsumed);
        BigDecimal fixedCharges = tariff.getFixedCharge();
        BigDecimal meterRent = tariff.getMeterRent();

        BigDecimal subtotal = energyCharges.add(fixedCharges).add(meterRent);

        BigDecimal electricityDuty = calculateAdditionalCharge(subtotal, "Electricity Duty", account.getTariffCategory());
        BigDecimal fuelAdjustment = calculateAdditionalCharge(subtotal, "Fuel Adjustment Charge", account.getTariffCategory());
        BigDecimal wheelingCharges = calculateAdditionalCharge(subtotal, "Wheeling Charges", account.getTariffCategory());

        BigDecimal grossAmount = subtotal.add(electricityDuty).add(fuelAdjustment).add(wheelingCharges);

        BigDecimal subsidyAmount = calculateSubsidy(account, unitsConsumed, grossAmount, billDate);
        BigDecimal totalAmount = grossAmount.subtract(subsidyAmount).max(BigDecimal.ZERO);

        BigDecimal previousDue = getPreviousDue(account.getAccountId());
        BigDecimal lateFee = calculateAccruedLateFee(account, billDate);

        LateFeePolicy applicablePolicy = resolveLateFeePolicy(account, billDate).orElse(null);
        LocalDate dueDate = determineDueDate(applicablePolicy, billDate);

        BigDecimal netPayable = totalAmount.add(previousDue).add(lateFee);

        String invoiceNumber = generateInvoiceNumber();

        Bill bill = new Bill();
        bill.setAccount(account);
        bill.setMeterReading(reading);
        bill.setInvoiceNumber(invoiceNumber);
        bill.setBillMonth(reading.getBillingMonth());
        bill.setBillDate(billDate);
        bill.setDueDate(dueDate);
        bill.setUnitsConsumed(unitsConsumed);
        bill.setEnergyCharges(energyCharges);
        bill.setFixedCharges(fixedCharges);
        bill.setMeterRent(meterRent);
        bill.setElectricityDuty(electricityDuty);
        bill.setOtherCharges(fuelAdjustment.add(wheelingCharges));
        bill.setSubsidyAmount(subsidyAmount);
        bill.setLateFee(lateFee);
        bill.setTotalAmount(totalAmount);
        bill.setPreviousDue(previousDue);
        bill.setNetPayable(netPayable);
        bill.setBalanceAmount(netPayable);
        bill.setBillStatus(Bill.BillStatus.UNPAID);
        bill.setGeneratedBy(generatedBy);

        Bill savedBill = billRepository.save(bill);

        // Auto-apply customer advance (wallet) to the newly created bill, if available
        try {
            com.msedcl.billing.shared.entity.Customer customer = account.getCustomer();
            java.math.BigDecimal advanceAvailable = java.math.BigDecimal.ZERO;
            if (customer.getAdvancePayment() != null && customer.getAdvancePayment() > 0) {
                advanceAvailable = java.math.BigDecimal.valueOf(customer.getAdvancePayment()).setScale(2, java.math.RoundingMode.HALF_UP);
            }

            java.math.BigDecimal outstanding = Optional.ofNullable(savedBill.getBalanceAmount()).orElse(savedBill.getNetPayable());

            if (advanceAvailable.compareTo(java.math.BigDecimal.ZERO) > 0 && outstanding.compareTo(java.math.BigDecimal.ZERO) > 0) {
                java.math.BigDecimal applyFromAdvance = advanceAvailable.min(outstanding).setScale(2, java.math.RoundingMode.HALF_UP);

                // deduct from customer wallet
                double newAdvance = customer.getAdvancePayment() - applyFromAdvance.doubleValue();
                customer.setAdvancePayment(newAdvance);
                customerRepository.save(customer);

                // update bill amounts/status
                java.math.BigDecimal safeAmountPaid = Optional.ofNullable(savedBill.getAmountPaid()).orElse(java.math.BigDecimal.ZERO);
                java.math.BigDecimal newAmountPaid = safeAmountPaid.add(applyFromAdvance).setScale(2, java.math.RoundingMode.HALF_UP);
                java.math.BigDecimal newBalance = Optional.ofNullable(savedBill.getBalanceAmount()).orElse(savedBill.getNetPayable()).subtract(applyFromAdvance).setScale(2, java.math.RoundingMode.HALF_UP);

                savedBill.setAmountPaid(newAmountPaid);
                savedBill.setBalanceAmount(newBalance.max(java.math.BigDecimal.ZERO));
                if (newBalance.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    savedBill.setBillStatus(Bill.BillStatus.PAID);
                } else {
                    savedBill.setBillStatus(Bill.BillStatus.PARTIALLY_PAID);
                }

                // Persist a payment record representing the advance application so there's a trace
                com.msedcl.billing.shared.entity.Payment adj = new com.msedcl.billing.shared.entity.Payment();
                adj.setBill(savedBill);
                adj.setAccount(savedBill.getAccount());
                adj.setPaymentAmount(applyFromAdvance);
                adj.setConvenienceFee(java.math.BigDecimal.ZERO);
                adj.setNetAmount(applyFromAdvance);
                adj.setPaymentMode(com.msedcl.billing.shared.entity.Payment.PaymentMode.CASH);
                adj.setPaymentChannel("ADVANCE_ADJUSTMENT");
                adj.setPaymentStatus(com.msedcl.billing.shared.entity.Payment.PaymentStatus.SUCCESS);
                adj.setPaymentDate(java.time.LocalDateTime.now());
                adj.setTransactionId("ADVANCE-ADJ-" + generateInvoiceNumber());
                adj.setPaymentReference("ADV-" + java.util.UUID.randomUUID().toString().substring(0,8).toUpperCase());
                adj.setProcessedBy(generatedBy);

                paymentRepository.save(adj);
                billRepository.save(savedBill);

                auditLogService.record(generatedBy,
                    "AUTO_APPLY_ADVANCE",
                    "Bill",
                    savedBill.getBillId(),
                    "Automatically applied advance payment of ₹" + applyFromAdvance + " from customer wallet to invoice " + savedBill.getInvoiceNumber(),
                    null);
            }
        } catch (Exception e) {
            // Do not fail bill generation for advance accounting errors
            System.err.println("Warning: failed to auto-apply advance to bill: " + e.getMessage());
        }

        try {
            String pdfPath = pdfService.generateBillPdf(savedBill);
            String qrPath = qrCodeService.generateQrCode(savedBill);

            savedBill.setPdfPath(pdfPath);
            savedBill.setQrCodePath(qrPath);

            billRepository.save(savedBill);
        } catch (Exception e) {
            System.err.println("Error generating PDF/QR: " + e.getMessage());
        }

        notificationService.sendBillGeneratedEmail(savedBill);

        auditLogService.record(generatedBy,
            "GENERATE_BILL",
            "Bill",
            savedBill.getBillId(),
            "Generated bill for account " + account.getAccountNumber() + " invoice " + invoiceNumber,
            null);

        return savedBill;
    }

    @Transactional
    public BillBatchGenerationResponse generateBillsForBillingMonth(String billingMonth, User generatedBy) {
        List<MeterReading> readings = meterReadingRepository.findByBillingMonth(billingMonth);

        int evaluated = readings.size();
        int created = 0;
        int skipped = 0;
        List<String> errors = new ArrayList<>();

        for (MeterReading reading : readings) {
            try {
                boolean alreadyGenerated = billRepository
                    .findByAccountAccountIdAndBillMonth(reading.getAccount().getAccountId(), billingMonth)
                    .isPresent();

                if (alreadyGenerated) {
                    skipped++;
                    continue;
                }

                generateBill(reading.getReadingId(), generatedBy);
                created++;
            } catch (Exception ex) {
                skipped++;
                errors.add(String.format(
                    "Account %s: %s",
                    reading.getAccount().getAccountNumber(),
                    ex.getMessage()
                ));
            }
        }

        return new BillBatchGenerationResponse(billingMonth, evaluated, created, skipped, errors);
    }

    private BigDecimal calculateEnergyCharges(Long tariffId, int unitsConsumed) {
        List<TariffSlab> slabs = tariffSlabRepository.findByTariffMaster_TariffIdOrderBySlabNumberAsc(tariffId);

        BigDecimal totalCharge = BigDecimal.ZERO;
        int remainingUnits = unitsConsumed;

        for (TariffSlab slab : slabs) {
            if (remainingUnits <= 0) break;

            int slabMin = slab.getMinUnits();
            Integer slabMax = slab.getMaxUnits();

            int unitsInSlab;
            if (slabMax == null) {
                unitsInSlab = remainingUnits;
            } else {
                int slabRange = slabMax - slabMin + 1;
                unitsInSlab = Math.min(remainingUnits, slabRange);
            }

            BigDecimal slabCharge = slab.getRatePerUnit().multiply(BigDecimal.valueOf(unitsInSlab));
            totalCharge = totalCharge.add(slabCharge);
            remainingUnits -= unitsInSlab;
        }

        return totalCharge.setScale(2, RoundingMode.HALF_UP);
    }

    private int resolveUnitsConsumed(MeterReading reading) {
        if (reading.getUnitsConsumed() != null) {
            return reading.getUnitsConsumed();
        }
        return Math.max(0, reading.getCurrentReading() - reading.getPreviousReading());
    }

    private BigDecimal calculateAdditionalCharge(BigDecimal baseAmount, String chargeName, String tariffCategory) {
        return additionalChargeRepository.findByIsActiveTrue().stream()
            .filter(charge -> chargeName.equalsIgnoreCase(charge.getChargeName()))
            .filter(charge -> {
                if (charge.getChargeName().startsWith("Convenience Fee")) {
                    return false;
                }
                String applicable = charge.getApplicableTo();
                return "ALL".equalsIgnoreCase(applicable) || applicable.contains(tariffCategory);
            })
            .map(charge -> {
                if (charge.getChargeType() == AdditionalCharge.ChargeType.PERCENTAGE) {
                    return baseAmount.multiply(charge.getChargeValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                }
                return charge.getChargeValue();
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSubsidy(Account account, int unitsConsumed, BigDecimal grossAmount, LocalDate billDate) {
        return subsidyRuleRepository.findActiveRules(account.getTariffCategory(), account.getConnectionType(), billDate).stream()
            .map(rule -> {
                int applicableUnits = unitsConsumed;
                if (rule.getMaxUnits() != null) {
                    applicableUnits = Math.min(applicableUnits, rule.getMaxUnits());
                }

                BigDecimal benefit = BigDecimal.ZERO;

                if (rule.getPerUnitSubsidy() != null && rule.getPerUnitSubsidy().compareTo(BigDecimal.ZERO) > 0) {
                    benefit = benefit.add(rule.getPerUnitSubsidy().multiply(BigDecimal.valueOf(applicableUnits)));
                }

                if (rule.getPercentageSubsidy() != null && rule.getPercentageSubsidy().compareTo(BigDecimal.ZERO) > 0) {
                    benefit = benefit.add(grossAmount.multiply(rule.getPercentageSubsidy())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
                }

                if (rule.getFixedSubsidy() != null && rule.getFixedSubsidy().compareTo(BigDecimal.ZERO) > 0) {
                    benefit = benefit.add(rule.getFixedSubsidy());
                }

                if (rule.getMaxBenefit() != null && rule.getMaxBenefit().compareTo(BigDecimal.ZERO) > 0) {
                    benefit = benefit.min(rule.getMaxBenefit());
                }

                return benefit.setScale(2, RoundingMode.HALF_UP);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAccruedLateFee(Account account, LocalDate asOfDate) {
        Optional<LateFeePolicy> policyOpt = resolveLateFeePolicy(account, asOfDate);
        if (policyOpt.isEmpty()) {
            BigDecimal previousDue = getPreviousDue(account.getAccountId());
            return previousDue.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(50.00) : BigDecimal.ZERO;
        }

        LateFeePolicy policy = policyOpt.get();

        return billRepository.findByAccountAccountIdOrderByBillDateDesc(account.getAccountId()).stream()
            .filter(bill -> bill.getBillStatus() == Bill.BillStatus.UNPAID || bill.getBillStatus() == Bill.BillStatus.PARTIALLY_PAID)
            .map(bill -> {
                int graceDays = Optional.ofNullable(policy.getGracePeriodDays()).orElse(0);
                LocalDate lateStart = bill.getDueDate().plusDays(graceDays);
                long overdueDays = ChronoUnit.DAYS.between(lateStart, asOfDate);
                if (overdueDays <= 0) {
                    return BigDecimal.ZERO;
                }

                BigDecimal outstanding = bill.getBalanceAmount();
                BigDecimal dailyRate = Optional.ofNullable(policy.getDailyRatePercentage()).orElse(BigDecimal.ZERO);
                BigDecimal percentageComponent = dailyRate.compareTo(BigDecimal.ZERO) > 0
                    ? outstanding.multiply(dailyRate)
                        .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(overdueDays))
                    : BigDecimal.ZERO;

                BigDecimal flatComponent = Optional.ofNullable(policy.getFlatFee()).orElse(BigDecimal.ZERO);

                BigDecimal totalLateFee = percentageComponent.add(flatComponent);

                if (policy.getMaxLateFee() != null && policy.getMaxLateFee().compareTo(BigDecimal.ZERO) > 0) {
                    totalLateFee = totalLateFee.min(policy.getMaxLateFee());
                }

                return totalLateFee.setScale(2, RoundingMode.HALF_UP);
            })
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private Optional<LateFeePolicy> resolveLateFeePolicy(Account account, LocalDate billDate) {
        List<LateFeePolicy> policies = lateFeePolicyRepository.findActivePolicies(account.getConnectionType(), billDate);
        return policies.stream().findFirst();
    }

    private LocalDate determineDueDate(LateFeePolicy policy, LocalDate billDate) {
        if (policy == null || policy.getStandardDueDays() == null) {
            return billDate.plusDays(15);
        }
        return billDate.plusDays(policy.getStandardDueDays());
    }

    private BigDecimal getPreviousDue(Long accountId) {
        List<Bill> previousBills = billRepository.findByAccountAccountIdOrderByBillDateDesc(accountId);
        return previousBills.stream()
            .filter(b -> b.getBillStatus() != Bill.BillStatus.PAID)
            .map(bill -> Optional.ofNullable(bill.getBalanceAmount()).orElse(BigDecimal.ZERO))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateInvoiceNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM");
        String datePrefix = LocalDate.now().format(formatter);
        long count = billRepository.count() + 1;
        return String.format("VIT/%s/%05d", datePrefix, count);
    }
}
