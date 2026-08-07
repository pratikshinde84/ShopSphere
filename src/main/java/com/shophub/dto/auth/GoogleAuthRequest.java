package com.shophub.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleAuthRequest {

    @NotBlank(message = "Google ID token is required")
    private String idToken;

    /**
     * Required only on first login (role selection screen).
     * On subsequent logins this field is ignored.
     */
    private String role;      // "BUYER" or "SELLER"

    /**
     * Required only on first login.
     */
    private String username;
}
