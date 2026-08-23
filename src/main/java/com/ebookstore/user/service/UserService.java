package com.ebookstore.user.service;

import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.user.UserMapper;
import com.ebookstore.user.dto.UserResponse;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user-profile operations.
 */
@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the authenticated user by database id and returns a DTO.
     *
     * @param userId the authenticated user's database id (from {@code AuthenticatedUser})
     * @throws ResourceNotFoundException if the user does not exist
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return UserMapper.toResponse(user);
    }
}
