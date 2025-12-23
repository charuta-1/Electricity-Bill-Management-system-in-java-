package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.MeterReading;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeterReadingSummaryDto {
    private Long readingId;
    private Long accountId;
    private String billingMonth;
    private LocalDate readingDate;
    private Integer previousReading;
    private Integer currentReading;
    private Integer unitsConsumed;
    private MeterReading.ReadingType readingType;
}
