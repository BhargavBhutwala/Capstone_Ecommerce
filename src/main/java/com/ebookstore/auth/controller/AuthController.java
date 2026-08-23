package com.ebookstore.auth.controller;

import com.ebookstore.auth.dto.LoginRequest;
import com.ebookstore.auth.dto.LoginResponse;
import com.ebookstore.auth.dto.RegisterRequest;
import com.ebookstore.auth.service.AuthService;
import com.ebookstore.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles authentication endpoints: register, login, logout.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** operationId: registerUser — public (no JWT required) */
    @Operation(operationId = "registerUser", summary = "Register a customer account")
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<UserResponse> registerUser(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** operationId: login — public (no JWT required) */
    @Operation(operationId = "login", summary = "Authenticate and obtain a JWT")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * operationId: logout
     *
     * <p>Stateless — no server-side token invalidation. The client discards the token.
     * JWT expiry remains the logout mechanism.
     */
    @Operation(operationId = "logout", summary = "Logout (client-side token discard)")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
