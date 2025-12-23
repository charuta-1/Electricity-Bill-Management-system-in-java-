package com.msedcl.billing.admin.customer.controller;

import com.msedcl.billing.shared.dto.RegisterRequest;
import com.msedcl.billing.admin.customer.dto.customer.CustomerResponse;
import com.msedcl.billing.shared.entity.Customer;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.customer.service.CustomerService;
import com.msedcl.billing.shared.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/admin/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;
    private final RegistrationService registrationService;

    // GET all customers
    @GetMapping
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    // GET a single customer by ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(customerService.getCustomerDetails(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    // POST (Create) a new customer
    @PostMapping
    public ResponseEntity<Object> createCustomer(@Valid @RequestBody RegisterRequest registerRequest,
                                            Authentication authentication,
                                            HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            CustomerResponse savedCustomer = registrationService.registerCustomerByAdmin(
                registerRequest,
                currentUser,
                request.getRemoteAddr()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCustomer);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    // PUT (Update) an existing customer
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer(@PathVariable Long id,
                                                   @RequestBody Customer customerDetails,
                                                   Authentication authentication,
                                                   HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Customer updatedCustomer = customerService.updateCustomer(id, customerDetails, currentUser, request.getRemoteAddr());
        return ResponseEntity.ok(customerService.toResponse(updatedCustomer));
    }

    // DELETE a customer
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable Long id,
                                            Authentication authentication,
                                            HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        customerService.deleteCustomer(id, currentUser, request.getRemoteAddr());
        return ResponseEntity.ok().build();
    }
}
