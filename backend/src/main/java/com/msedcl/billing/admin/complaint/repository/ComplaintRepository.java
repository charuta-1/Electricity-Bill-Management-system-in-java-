package com.msedcl.billing.admin.complaint.repository;

import com.msedcl.billing.shared.entity.Complaint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    Optional<Complaint> findByComplaintNumber(String complaintNumber);
    List<Complaint> findByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Complaint> findByAccountAccountIdOrderByCreatedAtDesc(Long accountId);
    List<Complaint> findByStatus(Complaint.Status status);
    List<Complaint> findByAssignedTo_UserId(Long userId);
    long countByStatus(Complaint.Status status);
    long countByCustomerCustomerIdAndStatusIn(Long customerId, Collection<Complaint.Status> statuses);
    List<Complaint> findTop5ByCustomerCustomerIdOrderByCreatedAtDesc(Long customerId);

    @EntityGraph(attributePaths = {"customer", "customer.user", "account", "assignedTo"})
    @Query("SELECT c FROM Complaint c ORDER BY c.createdAt DESC")
    List<Complaint> findAllWithDetails();
}
