package com.msedcl.billing.admin.customer.repository;

import com.msedcl.billing.shared.entity.Customer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerNumber(String customerNumber);
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByPhoneNumber(String phoneNumber);
    Optional<Customer> findByUserUserId(Long userId);
    Optional<Customer> findByAadharNumber(String aadharNumber);
    Boolean existsByCustomerNumber(String customerNumber);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @EntityGraph(attributePaths = {"user"})
    List<Customer> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user"})
    Optional<Customer> findWithUserByCustomerId(Long customerId);
}
