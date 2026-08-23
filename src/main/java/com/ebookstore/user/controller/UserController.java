package com.ebookstore.user.controller;

import com.ebookstore.security.AuthenticatedUser;
import com.ebookstore.user.dto.UserResponse;
import com.ebookstore.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Handles user-profile endpoints.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * operationId: getCurrentUser
     *
     * <p>The user id is obtained exclusively from the authenticated principal —
     * never from request parameters, request body, or JWT claims.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        AuthenticatedUser principal = (AuthenticatedUser) authentication.getPrincipal();
        UserResponse response = userService.getCurrentUser(principal.getId());
        return ResponseEntity.ok(response);
    }
}
