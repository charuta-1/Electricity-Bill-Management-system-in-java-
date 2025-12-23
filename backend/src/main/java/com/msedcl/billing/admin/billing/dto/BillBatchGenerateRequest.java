package com.msedcl.billing.admin.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BillBatchGenerateRequest(
    @NotBlank(message = "Billing month is required")
    @Pattern(regexp = "\\d{4}-\\d{2}", message = "Billing month must follow YYYY-MM format")
    String billingMonth
) {
}
