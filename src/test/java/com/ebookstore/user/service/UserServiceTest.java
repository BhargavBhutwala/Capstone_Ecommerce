package com.ebookstore.user.service;

import com.ebookstore.common.domain.UserRole;
import com.ebookstore.common.domain.UserStatus;
import com.ebookstore.common.exception.ResourceNotFoundException;
import com.ebookstore.user.dto.UserResponse;
import com.ebookstore.user.entity.User;
import com.ebookstore.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link UserService}.
 * No Spring context — UserRepository is mocked.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository);
    }

    @Test
    void getCurrentUser_returnsUserResponse_whenFound() {
        User user = buildUser(10L, "Bob", "Jones", "bob@example.com");
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        UserResponse result = userService.getCurrentUser(10L);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getEmail()).isEqualTo("bob@example.com");
        assertThat(result.getFirstName()).isEqualTo("Bob");
        assertThat(result.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void getCurrentUser_throwsResourceNotFoundException_whenMissing() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // =========================================================================
    // Helper
    // =========================================================================

    private User buildUser(Long id, String first, String last, String email) {
        User user = new User();
        user.setId(id);
        user.setFirstName(first);
        user.setLastName(last);
        user.setEmail(email);
        user.setRole(UserRole.CUSTOMER);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
