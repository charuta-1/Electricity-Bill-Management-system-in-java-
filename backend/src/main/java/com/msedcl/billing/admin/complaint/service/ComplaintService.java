package com.msedcl.billing.admin.complaint.service;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.Complaint;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.account.repository.AccountRepository;
import com.msedcl.billing.admin.complaint.repository.ComplaintRepository;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;

    public List<Complaint> getComplaintsForCustomer(Long customerId) {
        return complaintRepository.findByCustomerCustomerIdOrderByCreatedAtDesc(customerId);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllWithDetails();
    }

    public Complaint getComplaintById(Long id) {
        return complaintRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found with id: " + id));
    }

    @Transactional
    public Complaint createComplaint(Customer customer,
                                     Long accountId,
                                     Complaint.ComplaintType complaintType,
                                     Complaint.Priority priority,
                                     String subject,
                                     String description,
                                     User actor,
                                     String ipAddress) {

        Complaint complaint = new Complaint();
        complaint.setCustomer(customer);
        complaint.setComplaintType(complaintType);
        complaint.setPriority(priority != null ? priority : Complaint.Priority.MEDIUM);
        complaint.setSubject(StringUtils.hasText(subject) ? subject : complaintType.name() + " issue");
        complaint.setDescription(description);
        complaint.setStatus(Complaint.Status.OPEN);

        Account account = resolveAccount(customer, accountId);
        complaint.setAccount(account);

        complaint.setComplaintNumber(generateComplaintNumber());

        Complaint savedComplaint = complaintRepository.save(complaint);

        auditLogService.record(actor,
            "CREATE_COMPLAINT",
            "Complaint",
            savedComplaint.getComplaintId(),
            String.format("Created complaint %s for customer %s", savedComplaint.getComplaintNumber(), customer.getFullName()),
            ipAddress);

        return savedComplaint;
    }

    @Transactional
    public Complaint updateComplaint(Long id, Complaint complaintDetails, User actor, String ipAddress) {
        Complaint complaint = getComplaintById(id);

        complaint.setStatus(complaintDetails.getStatus());
        complaint.setResolution(complaintDetails.getResolution());
        complaint.setAssignedTo(complaintDetails.getAssignedTo());
        complaint.setPriority(complaintDetails.getPriority());
        complaint.setResolvedAt(complaintDetails.getResolvedAt());

        Complaint updatedComplaint = complaintRepository.save(complaint);

        auditLogService.record(actor,
            "UPDATE_COMPLAINT",
            "Complaint",
            updatedComplaint.getComplaintId(),
            String.format("Updated complaint %s (status: %s)", updatedComplaint.getComplaintNumber(), updatedComplaint.getStatus()),
            ipAddress);

        return updatedComplaint;
    }

    private String generateComplaintNumber() {
        return "CMP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private Account resolveAccount(Customer customer, Long accountId) {
        if (accountId != null) {
            return accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for complaint"));
        }

        return accountRepository.findByCustomerCustomerIdAndIsActiveTrue(customer.getCustomerId())
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No active account found for complaint"));
    }
}
