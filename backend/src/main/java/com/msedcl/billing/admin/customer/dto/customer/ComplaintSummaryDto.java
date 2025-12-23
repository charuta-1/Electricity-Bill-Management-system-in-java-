package com.msedcl.billing.admin.customer.dto.customer;

import com.msedcl.billing.shared.entity.Complaint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintSummaryDto {
    private Long complaintId;
    private Long accountId;
    private String complaintNumber;
    private Complaint.ComplaintType complaintType;
    private Complaint.Priority priority;
    private Complaint.Status status;
    private String subject;
    private String description;
    private LocalDateTime createdAt;
}
