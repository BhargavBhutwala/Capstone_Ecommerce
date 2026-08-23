package com.ebookstore.user.dto;

import com.ebookstore.common.domain.UserRole;
import com.ebookstore.common.domain.UserStatus;

import java.time.LocalDateTime;

/**
 * Response DTO representing a user. Matches the OpenAPI {@code UserResponse} schema.
 * Never exposes passwordHash.
 */
public class UserResponse {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final UserRole role;
    private final UserStatus status;
    private final LocalDateTime createdAt;

    public UserResponse(Long id,
                        String firstName,
                        String lastName,
                        String email,
                        UserRole role,
                        UserStatus status,
                        LocalDateTime createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId()                { return id; }
    public String getFirstName()       { return firstName; }
    public String getLastName()        { return lastName; }
    public String getEmail()           { return email; }
    public UserRole getRole()          { return role; }
    public UserStatus getStatus()      { return status; }
    public LocalDateTime getCreatedAt(){ return createdAt; }
}
