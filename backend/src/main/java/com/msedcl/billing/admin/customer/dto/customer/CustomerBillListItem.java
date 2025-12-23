package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Bill;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CustomerBillListItem(
    Long billId,
    String invoiceNumber,
    LocalDate billDate,
    LocalDate dueDate,
    Integer unitsConsumed,
    BigDecimal netPayable,
    BigDecimal balanceAmount,
    Bill.BillStatus billStatus,
    String pdfPath,
    String qrCodePath
) {
}
