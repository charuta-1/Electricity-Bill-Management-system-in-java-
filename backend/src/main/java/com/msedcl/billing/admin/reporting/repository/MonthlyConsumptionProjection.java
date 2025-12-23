package com.msedcl.billing.admin.reporting.repository;

public interface MonthlyConsumptionProjection {
    Integer getYear();
    Integer getMonth();
    Long getUnits();
}
