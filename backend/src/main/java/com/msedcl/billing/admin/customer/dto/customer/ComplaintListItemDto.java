package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Complaint;

import java.time.LocalDateTime;

public record ComplaintListItemDto(
    Long complaintId,
    String trackingNumber,
    Complaint.ComplaintType complaintType,
    Complaint.Priority priority,
    Complaint.Status status,
    String subject,
    LocalDateTime modifiedAt
) {
}
