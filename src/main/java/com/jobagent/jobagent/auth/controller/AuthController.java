package com.jobagent.jobagent.auth.controller;

import com.jobagent.jobagent.auth.dto.LoginRequest;
import com.jobagent.jobagent.auth.dto.LoginResponse;
import com.jobagent.jobagent.auth.dto.RegisterRequest;
import com.jobagent.jobagent.auth.dto.RegisterResponse;
import com.jobagent.jobagent.auth.model.User;
import com.jobagent.jobagent.auth.service.UserService;
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
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

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
