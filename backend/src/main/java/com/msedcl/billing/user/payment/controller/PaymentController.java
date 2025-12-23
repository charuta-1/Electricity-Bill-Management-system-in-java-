package com.msedcl.billing.user.payment.controller;

import com.msedcl.billing.user.payment.dto.PaymentRequest;
import com.msedcl.billing.shared.entity.Payment;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.user.payment.repository.PaymentRepository;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.user.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    @PostMapping("/payments")
    public ResponseEntity<?> recordPayment(@RequestBody PaymentRequest paymentRequest,
                                           Authentication authentication,
                                           HttpServletRequest request) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Payment savedPayment = paymentService.recordPayment(paymentRequest, currentUser, request.getRemoteAddr());
            return ResponseEntity.ok(savedPayment);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/admin/payments/bill/{billId}")
    public ResponseEntity<List<Payment>> getPaymentsForBill(@PathVariable Long billId) {
        return ResponseEntity.ok(paymentRepository.findByBill_BillIdOrderByPaymentDateDesc(billId));
    }

    @PostMapping("/customer/advance-payment")
    public ResponseEntity<?> addAdvancePayment(@RequestBody AdvancePaymentRequest request,
                                                Authentication authentication,
                                                HttpServletRequest httpRequest) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            paymentService.addAdvancePayment(request.amount(), currentUser, httpRequest.getRemoteAddr());
            return ResponseEntity.ok(new AdvancePaymentResponse("Advance payment added successfully", request.amount()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    // Admin: add advance payment to a specific customer
    @PostMapping("/admin/customers/{customerId}/advance-payment")
    public ResponseEntity<?> addAdvancePaymentForCustomer(@PathVariable Long customerId,
                                                           @RequestBody AdvancePaymentRequest request,
                                                           Authentication authentication,
                                                           HttpServletRequest httpRequest) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            paymentService.addAdvancePaymentForCustomer(customerId, request.amount(), currentUser, httpRequest.getRemoteAddr());
            return ResponseEntity.ok(new AdvancePaymentResponse("Advance payment added to customer", request.amount()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/customer/advance-payment")
    public ResponseEntity<?> getAdvancePayment(Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        Double advancePayment = paymentService.getAdvancePayment(currentUser);
        return ResponseEntity.ok(new AdvancePaymentResponse("Current advance payment", advancePayment));
    }

    // DTOs for advance payment
    public record AdvancePaymentRequest(Double amount) {}
    public record AdvancePaymentResponse(String message, Double balance) {}
}
