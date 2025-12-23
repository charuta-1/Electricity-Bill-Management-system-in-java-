package com.msedcl.billing.shared;

import com.msedcl.billing.shared.dto.AuthResponse;
import com.msedcl.billing.shared.dto.LoginRequest;
import com.msedcl.billing.shared.dto.RegisterRequest;
import com.msedcl.billing.shared.entity.User;
import com.msedcl.billing.shared.repository.UserRepository;
import com.msedcl.billing.shared.security.JwtUtil;
import com.msedcl.billing.shared.service.RegistrationService;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RegistrationService registrationService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );

            User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getUserId());

            AuthResponse response = new AuthResponse(
                token,
                user.getUserId(),
                user.getUsername(),
                user.getRole().name(),
                user.getFullName()
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            AuthResponse response = registrationService.registerCustomer(registerRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @GetMapping("/admin/users")
    public ResponseEntity<?> listAdmins(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(
            registrationService.listAdmins().stream()
                .map(AdminUserResponse::from)
                .toList()
        );
    }

    @PostMapping("/admin/users")
    public ResponseEntity<?> createAdmin(@Valid @RequestBody AdminUserRequest request,
                                         Authentication authentication,
                                         @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                         @RequestHeader(value = "X-Real-IP", required = false) String realIp,
                                         @RequestHeader(value = "X-Client-IP", required = false) String clientIp) {
        User actor = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        String ip = resolveClientIp(forwardedFor, realIp, clientIp);

        try {
            User admin = registrationService.createAdmin(
                request.getUsername(),
                request.getEmail(),
                request.getFullName(),
                request.getPhoneNumber(),
                request.getPassword(),
                request.getActive() == null || request.getActive(),
                actor,
                ip
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(AdminUserResponse.from(admin));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PatchMapping("/admin/users/{adminId}/status")
    public ResponseEntity<?> updateAdminStatus(@PathVariable Long adminId,
                                               @RequestParam("active") boolean active,
                                               Authentication authentication,
                                               @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
                                               @RequestHeader(value = "X-Real-IP", required = false) String realIp,
                                               @RequestHeader(value = "X-Client-IP", required = false) String clientIp) {
        User actor = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        String ip = resolveClientIp(forwardedFor, realIp, clientIp);

        try {
            User updated = registrationService.updateAdminStatus(adminId, active, actor, ip);
            return ResponseEntity.ok(AdminUserResponse.from(updated));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        if (token != null && token.startsWith("Bearer ")) {
            String jwt = token.substring(7);
            if (jwtUtil.validateToken(jwt)) {
                return ResponseEntity.ok("Token is valid");
            }
        }
        return ResponseEntity.status(401).body("Invalid token");
    }

    private String resolveClientIp(String forwardedFor, String realIp, String clientIp) {
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        if (clientIp != null && !clientIp.isBlank()) {
            return clientIp.trim();
        }
        return "unknown";
    }

    @Data
    @NoArgsConstructor
    public static class AdminUserRequest {
        @ValidUsername
        String username;
        @jakarta.validation.constraints.Email
        String email;
        @jakarta.validation.constraints.NotBlank
        String fullName;
        String phoneNumber;
        @jakarta.validation.constraints.NotBlank
        String password;
        Boolean active;
    }

    @Data
    @Builder
    public static class AdminUserResponse {
        Long userId;
        String username;
        String email;
        String fullName;
        String phoneNumber;
        boolean active;
        String role;
        LocalDateTime createdAt;
        LocalDateTime updatedAt;

        public static AdminUserResponse from(User user) {
            return AdminUserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .active(Boolean.TRUE.equals(user.getIsActive()))
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
        }
    }

    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @Retention(RetentionPolicy.RUNTIME)
    @jakarta.validation.Constraint(validatedBy = {})
    @jakarta.validation.constraints.Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$", message = "Username may contain letters, numbers, dots, hyphen or underscore")
    public @interface ValidUsername {}
}
