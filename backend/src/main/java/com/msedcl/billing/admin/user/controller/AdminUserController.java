package com.msedcl.billing.admin.user.controller;

import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.shared.service.RegistrationService;
import com.msedcl.billing.shared.AuthController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final RegistrationService registrationService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<AuthController.AdminUserResponse>> listAdmins(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
            registrationService.listAdmins().stream()
                .map(AuthController.AdminUserResponse::from)
                .toList()
        );
    }

    @PostMapping
    public ResponseEntity<?> createAdmin(@Valid @RequestBody AuthController.AdminUserRequest request,
                                         Authentication authentication,
                                         HttpServletRequest servletRequest) {
        User actor = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        try {
            String ipAddress = resolveClientIp(servletRequest);
            User admin = registrationService.createAdmin(
                request.getUsername(),
                request.getEmail(),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getPassword(),
                request.getActive() == null || request.getActive(),
                actor,
                ipAddress
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(AuthController.AdminUserResponse.from(admin));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PatchMapping("/{adminId}/status")
    public ResponseEntity<?> updateAdminStatus(@PathVariable Long adminId,
                                               @RequestParam("active") boolean active,
                                               Authentication authentication,
                                               HttpServletRequest servletRequest) {
        User actor = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        try {
            String ipAddress = resolveClientIp(servletRequest);
            User updated = registrationService.updateAdminStatus(adminId, active, actor, ipAddress);
            return ResponseEntity.ok(AuthController.AdminUserResponse.from(updated));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        String clientIp = request.getHeader("X-Client-IP");
        if (clientIp != null && !clientIp.isBlank()) {
            return clientIp.trim();
        }
        return request.getRemoteAddr();
    }
}
