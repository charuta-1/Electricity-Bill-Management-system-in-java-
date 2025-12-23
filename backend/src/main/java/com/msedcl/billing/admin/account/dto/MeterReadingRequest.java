package com.msedcl.billing.admin.account.dto;

import lombok.Data;

@Data
public class MeterReadingRequest {
    private Long accountId;
    private Integer currentReading;
    private String billingMonth;
    private String readingType;
    private String remarks;
}
