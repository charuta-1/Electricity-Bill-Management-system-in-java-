package com.msedcl.billing.admin.customer.service;

import com.msedcl.billing.admin.customer.dto.customer.CustomerResponse;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.admin.customer.repository.CustomerRepository;
import com.msedcl.billing.admin.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private static final DateTimeFormatter NUMBER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMM");

    private final CustomerRepository customerRepository;
    private final AuditLogService auditLogService;

    public List<CustomerResponse> getAllCustomers() {
        return customerRepository.findAllByOrderByCreatedAtDesc()
            .stream()
            .map(CustomerResponse::from)
            .toList();
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    public CustomerResponse getCustomerDetails(Long id) {
    return customerRepository.findWithUserByCustomerId(id)
            .map(CustomerResponse::from)
            .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }

    @Transactional
    public Customer createCustomer(Customer customer, User actor, String ipAddress) {
        customer.setCustomerNumber(generateCustomerNumber());
        Customer savedCustomer = customerRepository.save(customer);

        auditLogService.record(actor,
            "CREATE_CUSTOMER",
            "Customer",
            savedCustomer.getCustomerId(),
            String.format("Created customer %s (%s)", savedCustomer.getFullName(), savedCustomer.getCustomerNumber()),
            ipAddress);

        return savedCustomer;
    }

    @Transactional
    public Customer registerCustomerProfile(Customer customer) {
        customer.setCustomerNumber(generateCustomerNumber());
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerDetails, User actor, String ipAddress) {
        Customer customer = getCustomerById(id);

        customer.setFullName(customerDetails.getFullName());
        customer.setEmail(customerDetails.getEmail());
        customer.setPhoneNumber(customerDetails.getPhoneNumber());
        customer.setAddress(customerDetails.getAddress());
        customer.setCity(customerDetails.getCity());
        customer.setState(customerDetails.getState());
        customer.setPincode(customerDetails.getPincode());
        customer.setAadharNumber(customerDetails.getAadharNumber());
        customer.setAreaDetails(customerDetails.getAreaDetails());
        customer.setAdvancePayment(customerDetails.getAdvancePayment());

        Customer updatedCustomer = customerRepository.save(customer);

        auditLogService.record(actor,
            "UPDATE_CUSTOMER",
            "Customer",
            updatedCustomer.getCustomerId(),
            String.format("Updated customer %s (%s)", updatedCustomer.getFullName(), updatedCustomer.getCustomerNumber()),
            ipAddress);

        return updatedCustomer;
    }

    @Transactional
    public void deleteCustomer(Long id, User actor, String ipAddress) {
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);

        auditLogService.record(actor,
            "DELETE_CUSTOMER",
            "Customer",
            id,
            String.format("Deleted customer %s (%s)", customer.getFullName(), customer.getCustomerNumber()),
            ipAddress);
    }

    public CustomerResponse toResponse(Customer customer) {
        return getCustomerDetails(customer.getCustomerId());
    }

    private String generateCustomerNumber() {
        String datePart = LocalDate.now().format(NUMBER_DATE_FORMAT);
        String randomPart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        return "CUST-" + datePart + "-" + randomPart;
    }
}
