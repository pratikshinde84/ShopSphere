package com.shophub.controller;

import com.shophub.auth.AuthService;
import com.shophub.dto.ApiResponse;
import com.shophub.dto.auth.AuthResponse;
import com.shophub.dto.auth.GoogleAuthRequest;
import com.shophub.dto.auth.UserDto;
import com.shophub.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Google OAuth2 login and profile")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/google")
    @Operation(summary = "Login or register with Google ID Token")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(
            @Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile")
    public ResponseEntity<ApiResponse<UserDto>> getMe(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getMe(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
