package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;

import java.time.LocalDateTime;

public record CustomerResponse(
    Long customerId,
    String customerNumber,
    String fullName,
    String email,
    String phoneNumber,
    String address,
    String city,
    String state,
    String pincode,
    String aadharNumber,
    Long areaId,
    String areaName,
    String transformerNo,
    String feederNo,
    String poleNo,
    Double advancePayment,
    Long userId,
    String username,
    Boolean userActive,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CustomerResponse from(Customer customer) {
        User user = customer.getUser();
        var areaDetails = customer.getAreaDetails();

        return new CustomerResponse(
            customer.getCustomerId(),
            customer.getCustomerNumber(),
            customer.getFullName(),
            customer.getEmail(),
            customer.getPhoneNumber(),
            customer.getAddress(),
            customer.getCity(),
            customer.getState(),
            customer.getPincode(),
            customer.getAadharNumber(),
            areaDetails != null ? areaDetails.getId() : null,
            areaDetails != null ? areaDetails.getAreaName() : null,
            areaDetails != null ? areaDetails.getTransformerNo() : null,
            areaDetails != null ? areaDetails.getFeederNo() : null,
            areaDetails != null ? areaDetails.getPoleNo() : null,
            customer.getAdvancePayment(),
            user != null ? user.getUserId() : null,
            user != null ? user.getUsername() : null,
            user != null ? user.getIsActive() : null,
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
