package com.msedcl.billing.admin.billing.controller;

import com.msedcl.billing.admin.billing.dto.BillBatchGenerateRequest;
import com.msedcl.billing.admin.billing.dto.BillBatchGenerationResponse;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.billing.service.BillingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/bills")
@RequiredArgsConstructor
public class BillingBatchController {

    private final BillingService billingService;
    private final UserRepository userRepository;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> generateBills(
        @Valid @RequestBody BillBatchGenerateRequest request,
        Authentication authentication
    ) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));

        BillBatchGenerationResponse result = billingService.generateBillsForBillingMonth(
            request.billingMonth(),
            currentUser
        );

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", String.format("Processed %d readings and created %d bills for %s",
            result.readingsEvaluated(), result.billsCreated(), result.billingMonth()));
        payload.put("summary", result);

        if (!result.errors().isEmpty()) {
            payload.put("errors", result.errors());
        }

        return ResponseEntity.ok(payload);
    }
}
