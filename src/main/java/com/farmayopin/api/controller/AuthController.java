package com.farmayopin.api.controller;

import com.farmayopin.api.dto.auth.AuthResponse;
import com.farmayopin.api.dto.auth.LoginRequest;
import com.farmayopin.api.dto.auth.LogoutResponse;
import com.farmayopin.api.dto.auth.RegisterRequest;
import com.farmayopin.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        LogoutResponse response = authService.logout(authHeader);
        return ResponseEntity.ok(response);
    }
}
