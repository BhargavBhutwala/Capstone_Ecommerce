package com.ebookstore.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for {@code POST /auth/register}.
 * Validation constraints match the OpenAPI RegisterRequest schema exactly.
 */
public class RegisterRequest {

    @NotBlank(message = "firstName is required")
    @Size(min = 1, max = 100, message = "firstName must be between 1 and 100 characters")
    private String firstName;

    @NotBlank(message = "lastName is required")
    @Size(min = 1, max = 100, message = "lastName must be between 1 and 100 characters")
    private String lastName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 255, message = "email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
    private String password;

    public RegisterRequest() {}

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
