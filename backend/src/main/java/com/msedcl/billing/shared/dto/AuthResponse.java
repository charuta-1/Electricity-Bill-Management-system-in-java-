package com.msedcl.billing.shared.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type = "Bearer";
    private Long userId;
    private String username;
    private String role;
    private String fullName;

    public AuthResponse(String token, Long userId, String username, String role, String fullName) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
    }
}
