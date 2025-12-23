package com.msedcl.billing.admin.customer.dto.customer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerProfileDto {
    private Long customerId;
    private String customerNumber;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Long areaId;
    private String areaName;
    private String transformerNo;
    private String feederNo;
    private String poleNo;
    private Double advancePayment;
}
