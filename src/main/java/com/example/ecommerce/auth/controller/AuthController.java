package com.example.ecommerce.auth.controller;

import com.example.ecommerce.auth.dto.AuthResponse;
import com.example.ecommerce.auth.dto.LoginRequest;
import com.example.ecommerce.auth.dto.LogoutRequest;
import com.example.ecommerce.auth.dto.RefreshTokenRequest;
import com.example.ecommerce.auth.dto.RegisterRequest;
import com.example.ecommerce.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {

        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {

        AuthResponse response = authService.refreshAccessToken(
                request.getRefreshToken());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody LogoutRequest request) {

        authService.logout(request.getRefreshToken());

        return ResponseEntity.noContent().build();
    }
}