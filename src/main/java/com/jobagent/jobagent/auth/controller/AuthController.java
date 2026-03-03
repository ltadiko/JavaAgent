package com.jobagent.jobagent.auth.controller;

import com.jobagent.jobagent.auth.dto.LoginRequest;
import com.jobagent.jobagent.auth.dto.LoginResponse;
import com.jobagent.jobagent.auth.dto.RegisterRequest;
import com.jobagent.jobagent.auth.dto.RegisterResponse;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Sprint 1.6 — Authentication REST controller.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "User registration, login, and profile endpoints")
public class AuthController {

    private final UserService userService;

    @Operation(summary = "Register a new user", description = "Creates a new user account with email/password credentials and assigns a data residency region based on the country code",
            responses = {
                @ApiResponse(responseCode = "201", description = "User registered successfully"),
                @ApiResponse(responseCode = "400", description = "Validation error"),
                @ApiResponse(responseCode = "409", description = "Email already registered")
            })
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Login with credentials", description = "Authenticates a user and returns a JWT access token",
            responses = {
                @ApiResponse(responseCode = "200", description = "Login successful"),
                @ApiResponse(responseCode = "401", description = "Invalid credentials")
            })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get current user profile", description = "Returns the profile of the currently authenticated user based on JWT token",
            responses = {
                @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
                @ApiResponse(responseCode = "401", description = "Not authenticated")
            })
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userService.getUserById(userId);
        return ResponseEntity.ok(Map.of(
                "id", user.getId().toString(),
                "fullName", user.getFullName(),
                "email", user.getEmail(),
                "region", user.getRegion()
        ));
    }
}
