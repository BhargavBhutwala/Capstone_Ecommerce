package com.ebookstore.user;

import com.ebookstore.user.dto.UserResponse;
import com.ebookstore.user.entity.User;

/**
 * Hand-written mapper: {@link User} entity → {@link UserResponse} DTO.
 * Never exposes passwordHash.
 */
public final class UserMapper {

    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
