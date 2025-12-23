package com.msedcl.billing.admin.complaint.controller;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.Complaint;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.complaint.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;
import java.util.Map;
import java.util.List;
import org.springframework.util.StringUtils;

@RestController
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    // Endpoint for customers to create a complaint
    @PostMapping("/customer/complaints")
    public ResponseEntity<?> createComplaint(@RequestBody Map<String, Object> payload,
                                             Authentication authentication,
                                             HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Customer customer = customerRepository.findByUserUserId(currentUser.getUserId())
            .orElseThrow(() -> new RuntimeException("Customer profile not found"));

        try {
            Complaint.ComplaintType complaintType = resolveComplaintType(payload.get("complaintType"));
            Complaint.Priority priority = resolvePriority(payload.get("priority"));
            Long accountId = resolveAccountId(payload.get("accountId"));
            String subject = trimToNull(payload.get("subject"));
            String description = trimToNull(payload.get("description"));

            if (!StringUtils.hasText(description)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Description is required");
            }

            Complaint savedComplaint = complaintService.createComplaint(
                customer,
                accountId,
                complaintType,
                priority,
                subject,
                description,
                currentUser,
                request.getRemoteAddr()
            );

            return ResponseEntity.ok(savedComplaint);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    // Endpoint for customers to view their own complaints
    @GetMapping("/customer/complaints")
    public ResponseEntity<List<Complaint>> getCustomerComplaints(Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Long customerId = customerRepository.findByUserUserId(currentUser.getUserId())
            .map(Customer::getCustomerId)
            .orElseThrow(() -> new RuntimeException("Customer profile not found"));

        List<Complaint> complaints = complaintService.getComplaintsForCustomer(customerId);
        return ResponseEntity.ok(complaints);
    }

    // Endpoint for admins to view all complaints
    @GetMapping("/admin/complaints")
    public ResponseEntity<List<ComplaintSummaryResponse>> getAllComplaints() {
        List<ComplaintSummaryResponse> payload = complaintService.getAllComplaints().stream()
            .map(ComplaintSummaryResponse::fromComplaint)
            .toList();
        return ResponseEntity.ok(payload);
    }

    // Endpoint for admins to get a specific complaint
    @GetMapping("/admin/complaints/{id}")
    public ResponseEntity<Complaint> getComplaintById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(complaintService.getComplaintById(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // Endpoint for admins to update a complaint
    @PutMapping("/admin/complaints/{id}")
    public ResponseEntity<?> updateComplaint(@PathVariable Long id,
                                                     @RequestBody Complaint complaintDetails,
                                                     Authentication authentication,
                                                     HttpServletRequest request) {
        try {
            User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            Complaint updatedComplaint = complaintService.updateComplaint(id, complaintDetails, currentUser, request.getRemoteAddr());
            return ResponseEntity.ok(ComplaintSummaryResponse.fromComplaint(updatedComplaint));
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage()));
        }
    }

    private record ErrorResponse(String message) {
    }

    private Complaint.ComplaintType resolveComplaintType(Object rawValue) {
        if (rawValue == null) {
            return Complaint.ComplaintType.BILLING;
        }

        String text = rawValue.toString().trim();
        if (!StringUtils.hasText(text)) {
            return Complaint.ComplaintType.BILLING;
        }

        String normalized = text.toUpperCase(Locale.ROOT);
        if ("SERVICE".equals(normalized)) {
            return Complaint.ComplaintType.OTHER;
        }

        try {
            return Complaint.ComplaintType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid complaint type: " + text);
        }
    }

    private Complaint.Priority resolvePriority(Object rawValue) {
        if (rawValue == null) {
            return Complaint.Priority.MEDIUM;
        }

        String text = rawValue.toString().trim();
        if (!StringUtils.hasText(text)) {
            return Complaint.Priority.MEDIUM;
        }

        try {
            return Complaint.Priority.valueOf(text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid priority: " + text);
        }
    }

    private Long resolveAccountId(Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        if (rawValue instanceof Number number) {
            return number.longValue();
        }

        String text = rawValue.toString().trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }

        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid accountId: " + text);
        }
    }

    private String trimToNull(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String text = rawValue.toString().trim();
        return StringUtils.hasText(text) ? text : null;
    }

    public static record ComplaintSummaryResponse(
        Long complaintId,
        String complaintNumber,
        String complaintType,
        String subject,
        String description,
        String priority,
        String status,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime updatedAt,
        CustomerSummary customer,
        AccountSummary account,
        UserSummary assignedTo
    ) {

        static ComplaintSummaryResponse fromComplaint(Complaint complaint) {
            if (complaint == null) {
                return null;
            }

            return new ComplaintSummaryResponse(
                complaint.getComplaintId(),
                complaint.getComplaintNumber(),
                complaint.getComplaintType() != null ? complaint.getComplaintType().name() : null,
                complaint.getSubject(),
                complaint.getDescription(),
                complaint.getPriority() != null ? complaint.getPriority().name() : null,
                complaint.getStatus() != null ? complaint.getStatus().name() : null,
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                CustomerSummary.fromCustomer(complaint.getCustomer()),
                AccountSummary.fromAccount(complaint.getAccount()),
                UserSummary.fromUser(complaint.getAssignedTo())
            );
        }

        public record CustomerSummary(
            Long customerId,
            String customerNumber,
            String fullName,
            String phoneNumber
        ) {
            static CustomerSummary fromCustomer(Customer customer) {
                if (customer == null) {
                    return null;
                }
                return new CustomerSummary(
                    customer.getCustomerId(),
                    customer.getCustomerNumber(),
                    customer.getFullName(),
                    customer.getPhoneNumber()
                );
            }
        }

        public record AccountSummary(
            Long accountId,
            String accountNumber,
            String meterNumber,
            String connectionType
        ) {
            static AccountSummary fromAccount(Account account) {
                if (account == null) {
                    return null;
                }
                return new AccountSummary(
                    account.getAccountId(),
                    account.getAccountNumber(),
                    account.getMeterNumber(),
                    account.getConnectionType() != null ? account.getConnectionType().name() : null
                );
            }
        }

        public record UserSummary(
            Long userId,
            String username,
            String fullName
        ) {
            static UserSummary fromUser(User user) {
                if (user == null) {
                    return null;
                }
                return new UserSummary(
                    user.getUserId(),
                    user.getUsername(),
                    user.getFullName()
                );
            }
        }
    }
}
