package com.msedcl.billing.shared.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "bills")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bill_id")
    private Long billId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reading_id", nullable = false)
    private MeterReading meterReading;

    @Column(name = "invoice_number", unique = true, nullable = false, length = 50)
    private String invoiceNumber;

    @Column(name = "bill_month", nullable = false, length = 7)
    private String billMonth;

    @Column(name = "bill_date", nullable = false)
    private LocalDate billDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "units_consumed", nullable = false)
    private Integer unitsConsumed;

    @Column(name = "energy_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal energyCharges;

    @Column(name = "fixed_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal fixedCharges;

    @Column(name = "meter_rent", nullable = false, precision = 10, scale = 2)
    private BigDecimal meterRent;

    @Column(name = "electricity_duty", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityDuty;

    @Column(name = "other_charges", precision = 10, scale = 2)
    private BigDecimal otherCharges = BigDecimal.ZERO;

    @Column(name = "subsidy_amount", precision = 10, scale = 2)
    private BigDecimal subsidyAmount = BigDecimal.ZERO;

    @Column(name = "late_fee", precision = 10, scale = 2)
    private BigDecimal lateFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "previous_due", precision = 10, scale = 2)
    private BigDecimal previousDue = BigDecimal.ZERO;

    @Column(name = "net_payable", nullable = false, precision = 10, scale = 2)
    private BigDecimal netPayable;

    @Column(name = "amount_paid", precision = 10, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Column(name = "balance_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "bill_status")
    private BillStatus billStatus = BillStatus.UNPAID;

    @Column(name = "pdf_path")
    private String pdfPath;

    @Column(name = "qr_code_path")
    private String qrCodePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by")
    private User generatedBy;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Payment> payments;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum BillStatus {
        UNPAID, PARTIALLY_PAID, PAID, OVERDUE
    }
}
