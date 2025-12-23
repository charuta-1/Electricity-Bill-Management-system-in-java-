package com.msedcl.billing.admin.reporting.repository;

import java.math.BigDecimal;

public interface MonthlyCollectionProjection {
    Integer getYear();
    Integer getMonth();
    BigDecimal getTotalAmount();
}
