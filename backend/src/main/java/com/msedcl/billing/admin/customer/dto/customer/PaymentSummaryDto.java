package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSummaryDto {
    private Long paymentId;
    private Long billId;
    private Long accountId;
    private String paymentReference;
    private LocalDateTime paymentDate;
    private BigDecimal paymentAmount;
    private BigDecimal convenienceFee;
    private BigDecimal netAmount;
    private Payment.PaymentMode paymentMode;
    private Payment.PaymentStatus paymentStatus;
    private String paymentChannel;
}
