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
public class BillSummaryDto {
    private Long billId;
    private Long accountId;
    private String invoiceNumber;
    private String billMonth;
    private LocalDate billDate;
    private LocalDate dueDate;
    private Integer unitsConsumed;
    private BigDecimal netPayable;
    private BigDecimal amountPaid;
    private BigDecimal balanceAmount;
    private Bill.BillStatus status;
}
