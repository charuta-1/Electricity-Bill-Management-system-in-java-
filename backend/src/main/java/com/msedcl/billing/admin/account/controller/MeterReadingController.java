package com.msedcl.billing.admin.account.controller;

import com.msedcl.billing.admin.account.dto.MeterReadingRequest;
import com.msedcl.billing.admin.account.dto.meter.MeterReadingResponse;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.admin.account.service.MeterReadingAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/readings")
@RequiredArgsConstructor
public class MeterReadingController {

    private final MeterReadingAdminService meterReadingAdminService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<?> addMeterReading(@RequestBody MeterReadingRequest request, Authentication authentication) {
        try {
            User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

            MeterReadingResponse response = meterReadingAdminService.addMeterReading(request, currentUser);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new ErrorResponse("Error: " + e.getMessage()));
        }
    }

    private record ErrorResponse(String message) {
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<MeterReadingResponse>> getReadingsByAccount(@PathVariable Long accountId) {
        List<MeterReadingResponse> readings = meterReadingAdminService.getReadingsByAccount(accountId);
        return ResponseEntity.ok(readings);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeterReadingResponse> getReading(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(meterReadingAdminService.getReading(id));
        } catch (RuntimeException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
