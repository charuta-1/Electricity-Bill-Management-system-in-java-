package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Bill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillDetailResponse {
    private Long billId;
    private Long accountId;
    private String invoiceNumber;
    private String billMonth;
    private LocalDate billDate;
    private LocalDate dueDate;
    private Integer unitsConsumed;
    private BigDecimal energyCharges;
    private BigDecimal fixedCharges;
    private BigDecimal meterRent;
    private BigDecimal electricityDuty;
    private BigDecimal otherCharges;
    private BigDecimal subsidyAmount;
    private BigDecimal lateFee;
    private BigDecimal totalAmount;
    private BigDecimal previousDue;
    private BigDecimal netPayable;
    private BigDecimal amountPaid;
    private BigDecimal balanceAmount;
    private Bill.BillStatus status;
    private String pdfPath;
    private String qrCodePath;
}
