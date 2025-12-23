package com.msedcl.billing.admin.complaint.dto.complaint;

import com.msedcl.billing.shared.entity.Account;
import com.msedcl.billing.shared.entity.Complaint;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;

import java.time.LocalDateTime;
import java.util.Optional;

public record ComplaintSummaryResponse(
    Long complaintId,
    String complaintNumber,
    String complaintType,
    String subject,
    String description,
    String priority,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    CustomerSnippet customer,
    AccountSnippet account,
    UserSnippet assignedTo
) {

    public static ComplaintSummaryResponse fromEntity(Complaint complaint) {
        if (complaint == null) {
            return null;
        }

        Customer customer = complaint.getCustomer();
        Account account = complaint.getAccount();
        User assignedTo = complaint.getAssignedTo();

        return new ComplaintSummaryResponse(
            complaint.getComplaintId(),
            complaint.getComplaintNumber(),
            Optional.ofNullable(complaint.getComplaintType()).map(Enum::name).orElse(null),
            complaint.getSubject(),
            complaint.getDescription(),
            Optional.ofNullable(complaint.getPriority()).map(Enum::name).orElse(null),
            Optional.ofNullable(complaint.getStatus()).map(Enum::name).orElse(null),
            complaint.getCreatedAt(),
            complaint.getUpdatedAt(),
            CustomerSnippet.from(customer),
            AccountSnippet.from(account),
            UserSnippet.from(assignedTo)
        );
    }

    public record CustomerSnippet(
        Long customerId,
        String customerNumber,
        String fullName,
        String phoneNumber
    ) {
        static CustomerSnippet from(Customer customer) {
            if (customer == null) {
                return null;
            }
            return new CustomerSnippet(
                customer.getCustomerId(),
                customer.getCustomerNumber(),
                customer.getFullName(),
                customer.getPhoneNumber()
            );
        }
    }

    public record AccountSnippet(
        Long accountId,
        String accountNumber,
        String meterNumber,
        String connectionType
    ) {
        static AccountSnippet from(Account account) {
            if (account == null) {
                return null;
            }
            return new AccountSnippet(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getMeterNumber(),
                Optional.ofNullable(account.getConnectionType()).map(Enum::name).orElse(null)
            );
        }
    }

    public record UserSnippet(
        Long userId,
        String username,
        String fullName
    ) {
        static UserSnippet from(User user) {
            if (user == null) {
                return null;
            }
            return new UserSnippet(
                user.getUserId(),
                user.getUsername(),
                user.getFullName()
            );
        }
    }
}
