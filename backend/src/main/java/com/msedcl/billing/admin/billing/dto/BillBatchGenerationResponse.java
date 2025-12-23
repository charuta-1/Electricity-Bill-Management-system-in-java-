package com.msedcl.billing.admin.billing.dto;

import java.util.List;

public record BillBatchGenerationResponse(
    String billingMonth,
    int readingsEvaluated,
    int billsCreated,
    int skipped,
    List<String> errors
) {
}
