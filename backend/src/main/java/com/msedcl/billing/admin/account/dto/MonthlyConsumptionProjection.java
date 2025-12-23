package com.msedcl.billing.admin.account.dto;

public interface MonthlyConsumptionProjection {
    Integer getYear();
    Integer getMonth();
    Long getUnits();
}
