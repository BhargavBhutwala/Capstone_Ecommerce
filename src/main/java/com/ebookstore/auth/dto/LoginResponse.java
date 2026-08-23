package com.ebookstore.auth.dto;

import com.ebookstore.user.dto.UserResponse;

/**
 * Response body for {@code POST /auth/login}.
 */
public class LoginResponse {

    private final String accessToken;
    private final String tokenType;
    private final long expiresIn;
    private final UserResponse user;

    public LoginResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.user = user;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType()   { return tokenType; }
    public long getExpiresIn()     { return expiresIn; }
    public UserResponse getUser()  { return user; }
}
