package com.msedcl.billing.user.payment.dto;

import java.math.BigDecimal;

public interface MonthlyCollectionProjection {
    Integer getYear();
    Integer getMonth();
    BigDecimal getTotalAmount();
}
