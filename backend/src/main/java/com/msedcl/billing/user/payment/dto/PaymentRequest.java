package com.msedcl.billing.user.payment.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long billId;
    private BigDecimal paymentAmount;
    private String paymentMode;
    private String transactionId;
    private String chequeNumber;
    private String chequeDate;
    private String bankName;
    private String remarks;
    private String paymentChannel;
    private String upiReference;
}
